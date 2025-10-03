package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
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

    override val compressionTemplates = listOf(
        //ResizeWidth|FramerateFps
        "{{SCREEN_00}}|0",  //Screen width (2.3007243)
        "{{VIDEO_75}}|0",  //75% of video width (2.968316)
        "{{SCREEN_75}}|0",  //75% of screen width (4.029935)
        "{{VIDEO_50}}|0",  //50% of video width (6.4749637)
        "{{SCREEN_50}}|0",  //50% of screen width (8.694984)
        "{{VIDEO_50}}|20",  //50% of video width (8.694984)
        "{{SCREEN_50}}|20",  //50% of screen width (8.694984)
        "{{SCREEN_40}}|0",  //40% of video width (12.959882)
        "{{SCREEN_40}}|20",  //40% of video width (12.959882)
        "{{VIDEO_25}}|0",  //25% of video width (21.979757)
        "{{SCREEN_25}}|0",  //25% of screen width (28.050495)
        "{{VIDEO_25}}|20",  //25% of video width (21.979757)
        "{{SCREEN_25}}|20",  //25% of screen width (28.050495)
        "{{VIDEO_20}}|20",  //20% of screen width (37.987946)
        "{{SCREEN_20}}|20",  //10% of screen width (50.804916)
        "{{SCREEN_10}}|0",  //20% of screen width (37.987946)
        "{{SCREEN_10}}|20",  //10% of screen width (50.804916)
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
                val compressionTemplate = getCompressionTemplateForDesiredCompressionRatio(inputFilePath, outputFilePath, compressionRatio)

                //Compress full size file using appropriate compression level
                if (compressionTemplate != null) {
                    val success = compressInBackground(inputFilePath, outputFilePath, compressionTemplate)
                    return@runBlocking success
                } else {
                    Log.e(tag, "Error during video compression of $inputFilePath with compressionTemplate: $compressionTemplate")
                    return@runBlocking false
                }
            } catch (e: Exception) {
                Log.e(tag, "Error during video compression in coroutine: ${e.message}", e)
                return@runBlocking false // Return false on exception
            }
        }
    }

    private suspend fun getCompressionTemplateForDesiredCompressionRatio(inputFilePath: String, outputFilePath: String, compressionRatio: Int): String? {
        val videoInfo = videoInfo(inputFilePath)
        val mediaDurationMs = videoInfo[MediaMetadataRetriever.METADATA_KEY_DURATION]?.toFloat() ?: 0f
        val clipDurationSecs = if (mediaDurationMs >= 5000f) 5 else ceil(mediaDurationMs / 1000f).toInt()  //Clip duration up to 5 seconds (less for videos longer than 5 seconds)
        val clippedVideoPctOfFullDuration = (clipDurationSecs * 1000).toFloat() / (if (mediaDurationMs > 0f) mediaDurationMs else 1f)
        val maxCompressedSizeForClippedFile = (File(inputFilePath).length() * clippedVideoPctOfFullDuration / compressionRatio).toInt()
        Log.d(tag, "Max compressed size for clipped file '$inputFilePath': $maxCompressedSizeForClippedFile, full size: ${File(inputFilePath).length()}")

        var minTemplateIdx = 0
        var maxTemplateIdx = compressionTemplates.size - 1
        var templateIdx = compressionTemplates.size / 2
        var templateIdxForLargestCompressedSizeLEGoal = maxTemplateIdx
        var compressedClipSize = compressClipByTemplate(inputFilePath, outputFilePath, compressionRatio, compressionTemplates[templateIdx], clipDurationSecs)
        do {
            if (compressedClipSize <= maxCompressedSizeForClippedFile) {
                templateIdxForLargestCompressedSizeLEGoal = templateIdx
                maxTemplateIdx = templateIdx - 1
            } else {
                minTemplateIdx = templateIdx + 1
            }
            if (minTemplateIdx > maxTemplateIdx) break

            templateIdx = (minTemplateIdx + maxTemplateIdx) / 2
            compressedClipSize = compressClipByTemplate(inputFilePath, outputFilePath, compressionRatio, compressionTemplates[templateIdx], clipDurationSecs)
        } while (true)
        Log.d(tag, "Optimal template: ${compressionTemplates[templateIdxForLargestCompressedSizeLEGoal]}")
        return compressionTemplates[templateIdxForLargestCompressedSizeLEGoal]
    }

    private suspend fun compressClipByTemplate(inputFilePath: String, outputFilePath: String, compressionRatio: Int, compressionTemplate: String, clipDurationSecs: Int): Int {
        if (compressInBackground(inputFilePath, outputFilePath, compressionTemplate, clipDurationSecs * 1024L)) {
            val newSize = File(outputFilePath).length()
            Log.d(tag, "Video compression for $inputFilePath, output file size: $newSize, command: $compressionTemplate, compression: ${File(inputFilePath).length() / compressionRatio / newSize.toFloat()}")
            return newSize.toInt()
        } else {
            Log.e(tag, "Image compression failed for $inputFilePath")
            return 0
        }

    }
    @OptIn(UnstableApi::class)
    suspend fun compressInBackground(inputFilePath: String, outputFilePath: String, compressionCommand: String, clipDurationMs: Long = 0L): Boolean {
        var result = true
        withContext(Dispatchers.Main) { // Transformer requires Dispatchers.Main
            suspendCancellableCoroutine { continuation ->
                try {
                    val videoInfo = videoInfo(inputFilePath)
                    val tokens = xlateDesiredCompressionCommand(compressionCommand, inputFilePath, outputFilePath, videoInfo).split("|")
                    val videoWidth = videoInfo[MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH]?.toIntOrNull() ?: 0
                    val videoHeight = videoInfo[MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT]?.toIntOrNull() ?: 0
                    val videoSizeScale = tokens[0].toFloat() / if (videoWidth > videoHeight) videoWidth.toFloat() else videoHeight.toFloat()
                    val frameRateFps = tokens[1].toFloat()
                    val editedMediaItem = createEditedMediaItem(inputFilePath, clipDurationMs, videoSizeScale, frameRateFps)  //Input transformations

                    //Process transformer to create new file
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H265)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
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
}
