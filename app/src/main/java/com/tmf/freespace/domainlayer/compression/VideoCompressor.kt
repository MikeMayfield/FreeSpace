package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C.PRIORITY_PROCESSING_BACKGROUND
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Codec
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.tmf.freespace.datalayer.models.MediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.ceil


class VideoCompressor(context: Context) : ICompressor(context) {
    private val tag = VideoCompressor::class.simpleName

    override val compressionCommands = listOf(
        //ResizeWidth|FramerateFps|VideoBitrateKbps|AudioBitrateKbps
        "0|0|0|0",  //Compression low (just conversion to H.265)
        "{{SCREEN_WIDTH}}|0|0|0",  //Compression medium low (screen width, unchanged fps, unchanged video bitrate (and no VBR), unchanged bitrates)
        "{{SCREEN_WIDTH}}|0|2000|128",  //Compression medium (screen width, unchanged fps, 2mbps video, 128kbps audio)
        "{{SCREEN_WIDTH}}|24|1500|128",  //Compression medium high (screen width, 24fps, 1.5mbps video, 128kbps audio)
        "{{SCREEN_WIDTH_33PCT}}|24|1500|128",  //Compression high (33% screen width, 24fps, 1.5mbps video, 128kbps audio)
        "{{SCREEN_WIDTH_25PCT}}|20|750|24",  //Compression very high (25% screen width, 20fps, 750kbps video, 24kbps audio)
        "176|12|500|24",  //Compression very very high (176x144px, 12fps, 500kbps video, 24kbps audio)
    )

    /**
     * Compress a video file to create a new file of no more than maxCompressedSize bytes.
     */
    @OptIn(UnstableApi::class)
    override fun compress(mediaFile: MediaFile, outputFilePath: String, compressionRatio: Int): Boolean {
        // WARNING: runBlocking will block the current thread.
        // This is acceptable because 'compress' is already called from a background worker thread.
        // If 'compress' can be called from the main thread, this will cause ANR.
        // The ideal solution would be to make 'ICompressor.compress' a suspend function.
        return runBlocking {
            try {
                val inputFilePath = mediaFile.fullPath

                //Find compression level based on compression of 5 second clip to allow finding probable compression level more quickly than using full size file for each test
                val compressionCommand = getCompressionCommandForDesiredCompressionRatio(inputFilePath, outputFilePath, compressionRatio)

                //Compress full size file using appropriate compression level
                if (compressionCommand != null) {
                    return@runBlocking compressInBackground(inputFilePath, outputFilePath, compressionCommand)
                } else {
                    Log.e(tag, "Error during video compression with optimal compression level: $inputFilePath")
                    return@runBlocking false
                }
            } catch (e: Exception) {
                Log.e(tag, "Error during video compression in coroutine: ${e.message}", e)
                return@runBlocking false // Return false on exception
            }
        }
    }

    private suspend fun getCompressionCommandForDesiredCompressionRatio(inputFilePath: String, outputFilePath: String, compressionRatio: Int): String? {
//        val clipDurationSecs = 5
        val videoInfo = videoInfo(inputFilePath)
        val mediaDurationMs = videoInfo[MediaMetadataRetriever.METADATA_KEY_DURATION]?.toFloat() ?: 0f
        val clipDurationSecs = if (mediaDurationMs >= 5000f) 5 else ceil(mediaDurationMs / 1000f).toInt()  //Clip duration up to 5 seconds (less for videos longer than 5 seconds)
        val clippedVideoPctOfFullDuration = (clipDurationSecs * 1000).toFloat() / (if (mediaDurationMs > 0f) mediaDurationMs else 1f)
        val maxCompressedSizeForClippedFile = (File(inputFilePath).length() * clippedVideoPctOfFullDuration / compressionRatio).toInt()
        Log.d(tag, "Max compressed size for clipped file '$inputFilePath': $maxCompressedSizeForClippedFile, full size: ${File(inputFilePath).length()}")

        for (compressionCommand in compressionCommands) {
            if (compressInBackground(inputFilePath, outputFilePath, compressionCommand, clipDurationSecs * 1024L)) {
                Log.d(tag, "Image compression for $inputFilePath, output file size: ${File(outputFilePath).length()}, command: $compressionCommand")
                if (File(outputFilePath).length() <= maxCompressedSizeForClippedFile) {
                    return compressionCommand
                }
            } else {
                Log.e(tag, "Image compression failed for $inputFilePath")
                return null
            }
        }

        return compressionCommands[compressionCommands.size - 1]  //Default to highest possible compression if no compression makes file small enough
    }

    @OptIn(UnstableApi::class)
    suspend fun compressInBackground(inputFilePath: String, outputFilePath: String, compressionCommand: String, clipDurationMs: Long = 0L): Boolean {
        var result = true
        withContext(Dispatchers.Main) { // Transformer requires Dispatchers.Main
            suspendCancellableCoroutine { continuation ->
                try {
                    val tokens = xlateDesiredCompressionCommand(compressionCommand, inputFilePath, outputFilePath).split("|")
                    Log.v(tag, "Image compression command: $compressionCommand")
                    val videoInfo = videoInfo(inputFilePath)
                    val videoWidth = videoInfo[MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH]?.toIntOrNull() ?: 0
                    val videoHeight = videoInfo[MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT]?.toIntOrNull() ?: 0
                    val videoSizeScale = tokens[0].toFloat() / if (videoWidth > videoHeight) videoWidth.toFloat() else videoHeight.toFloat()
                    val frameRateFps = tokens[1].toFloat()
                    val actualVideoBitrateKbps = videoInfo[MediaMetadataRetriever.METADATA_KEY_BITRATE]?.toIntOrNull() ?: 0
                    var videoBitrateKbps = tokens[2].toInt()
                    if (videoBitrateKbps > actualVideoBitrateKbps) videoBitrateKbps = actualVideoBitrateKbps
                    val audioBitrateKbps = tokens[3].toInt()
//                    initOutputFile(outputFilePath)  //Delete existing output file //TODO Probably not needed
                    val editedMediaItem = createEditedMediaItem(inputFilePath, clipDurationMs, videoSizeScale, frameRateFps)  //Input transformations
                    val encoderFactory = createEncoderFactory(videoBitrateKbps, audioBitrateKbps)  //Output transformations

                    //Process transformer to create new file
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H265)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .setEncoderFactory(encoderFactory)
                        .experimentalSetTrimOptimizationEnabled(true)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                continuation.resume(exportResult)
                            }

                            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                result = false
                                Log.e(tag, "Video compression failed for '${inputFilePath}' with exportException: ${exportException.message}")
                                continuation.resumeWithException(exportException)
                            }
                        })
                        .build()
                    transformer.start(editedMediaItem, outputFilePath)
                }
                catch (e: Exception) {
                    Log.e(tag, "Error during video compression: ${e.message}")
                }
            }
        }

        return result
    }

    private fun videoInfo(filePath: String): Map<Int, String?> {
        val result = mutableMapOf<Int, String?>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val keyList = listOf(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH,
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT,
                MediaMetadataRetriever.METADATA_KEY_DURATION,
                MediaMetadataRetriever.METADATA_KEY_BITRATE,
            )
            for (key in keyList) {
                result[key] = retriever.extractMetadata(key)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error retrieving video width: ${e.message}")
            return result
        } finally {
            retriever.release()
        }

        return result
    }

    /**
     * Init output file, delete if exists
     */
    private fun initOutputFile(outputFilePath: String) {
        val outputFile = File(outputFilePath)
        if (outputFile.exists()) {
            outputFile.delete()
        }
    }

    /**
     * Create the EditedMediaItem with desired transformations for optionally shortening file and changing video resolution and frame rate
     *
     * @param inputFilePath The full path of the source file to compress
     */
    @OptIn(UnstableApi::class)
    private fun createEditedMediaItem(inputFilePath: String, endPositionMs: Long = 0L, videoSizeScale: Float = 0f, frameRate: Float = 0f): EditedMediaItem {
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(inputFilePath)
        if (endPositionMs > 0L) {  //Optional: End position to clip file to
            mediaItemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(0L)
                    .setEndPositionMs(endPositionMs)
                    .build()
            )
        }
        val mediaItem = mediaItemBuilder.build()

        //Set resized resolution (width x height)
        val videoEffectsList = arrayListOf<Effect>()
        if (videoSizeScale != 0f) {  //Optional: Scale video resolution (0 to 0.9999 as % of original resolution
            videoEffectsList.add(
                ScaleAndRotateTransformation.Builder()
                    .setScale(videoSizeScale, videoSizeScale)
                    .build()
            )
        }

        //Set video frame rate
        if (frameRate != 0f) {  //Optional: Set video frame rate
            videoEffectsList.add(
                FrameDropEffect.createDefaultFrameDropEffect(frameRate)
            )
        }

        val editedItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(listOf(), videoEffectsList))
            .build()

        return editedItem
    }

    @OptIn(UnstableApi::class)
    private fun createEncoderFactory(videoBitrateKbps: Int = 0, audioBitrateKbps: Int = 0) : Codec.EncoderFactory {
        //Set video bitrate and VBR
        val videoEncoderSettingsBuilder = VideoEncoderSettings.Builder()
        if (videoBitrateKbps > 0) {  //Optional: Change video bitrate
            videoEncoderSettingsBuilder.setBitrate(videoBitrateKbps * 1000)
            videoEncoderSettingsBuilder.setBitrateMode(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
        }
        val videoEncoderSettings = videoEncoderSettingsBuilder.build()

        //Set audio bitrate
        val audioEncoderSettingsBuilder = AudioEncoderSettings.Builder()
        if (audioBitrateKbps > 0) {  //Optional: Change audio bitrate
            audioEncoderSettingsBuilder.setBitrate(audioBitrateKbps * 1000)
        }
        val audioEncoderSettings = audioEncoderSettingsBuilder.build()

        //Create an encoder factory with desired settings
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedAudioEncoderSettings(audioEncoderSettings)
            .setRequestedVideoEncoderSettings(videoEncoderSettings)
            .setCodecPriority(PRIORITY_PROCESSING_BACKGROUND)
            .build()

        return encoderFactory
    }
}
