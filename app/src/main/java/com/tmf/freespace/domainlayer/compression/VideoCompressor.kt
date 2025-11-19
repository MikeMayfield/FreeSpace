package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.media.MediaMetadataRetriever
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
import com.tmf.freespace.domainlayer.general.DLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class VideoCompressor(context: Context) : ICompressor(context) {
    private val tag = "VideoCompressor"

    override val compressionTemplates = listOf(
        //ResizeWidth|Fps|H265
        "0|0|T",  //Just convert to H265 (must always be the first entry)
        "{{SCREEN_50}}|0|F",
        "{{SCREEN_40}}|0|F",
        "{{SCREEN_30}}|0|T",
        "{{SCREEN_30}}|0|F",
        "{{SCREEN_20}}|0|T",
        "{{SCREEN_20}}|0|F",
        "{{SCREEN_10}}|0|T",
        "{{SCREEN_10}}|0|F",
        "{{SCREEN_10}}|20|T",
        "{{SCREEN_10}}|20|F",
        "{{SCREEN_10}}|10|T",
        "{{SCREEN_10}}|10|F",
        "{{SCREEN_05}}|0|T",
        "{{SCREEN_05}}|0|F",
        "{{SCREEN_05}}|10|T",
        "{{SCREEN_05}}|10|F",
        "{{SCREEN_05}}|5|T",
        "{{SCREEN_05}}|5|F",
    )

    /**
     * Compress a video file to create a new file of no more than maxCompressedSize bytes.
     */
    @OptIn(UnstableApi::class)
    override fun compress(mediaFile: MediaFile, outputFilePath: String, compressionRatio: Int): Boolean {
        if (compressionRatio > 0) {
            // WARNING: runBlocking will block the current thread.
            // This is acceptable because 'compress' is already called from a background worker thread.
            // If 'compress' can be called from the main thread, this will cause ANR.
            // The ideal solution would be to make 'ICompressor.compress' a suspend function.
            return runBlocking {
                try {
                    val inputFilePath = mediaFile.fullPath

                    //Find compression level based on compression of 5 second clip to allow finding probable compression level more quickly than using full size file for each test
                    val compressionTemplate = getCompressionTemplateForDesiredCompressionRatio(inputFilePath, outputFilePath, compressionRatio)
                    DLog.d(tag, "Optimal template: $compressionTemplate")

                    //Compress full size file using appropriate compression level
                    if (compressionTemplate != null) {
                        val success = compressInBackground(inputFilePath, outputFilePath, compressionTemplate)
                        return@runBlocking success
                    } else {
                        DLog.e(tag, "Error during video compression of $inputFilePath with compressionTemplate: $compressionTemplate")
                        return@runBlocking false
                    }
                }
                catch (e: Exception) {
                    DLog.e(tag, "Error during video compression in coroutine: ${e.message}", e)
                    return@runBlocking false // Return false on exception
                }
            }
        }

        return true
    }

    private suspend fun getCompressionTemplateForDesiredCompressionRatio(inputFilePath: String, outputFilePath: String, compressionRatio: Int): String? {
        //Optimization for low compression ratio
        if (compressionRatio <= 1) {
            return compressionTemplates[0]  //Just convert to H265
        }

        val tempOutputFilePath = "$outputFilePath.tmp"
        var maxCompressionRatioSoFar = 0.0f
        var bestTemplateIdx = 0

        try {
            //Determine size of input file clipped to n seconds with no conversion. From that we can determine the max size of the file at the desired compression percentage
            val clipDurationSecs = 5
            val uncompressedClipSizeInt = compressClipByTemplate(inputFilePath, tempOutputFilePath, compressionTemplates[0], clipDurationSecs)
            val uncompressedClipSize = uncompressedClipSizeInt.toFloat()

            //Search for the optimal compression template: Lowest compression that yields the desired compression ratio
            for (templateIdx in 1..compressionTemplates.size - 1) {
                val compressedClipSize = compressClip(inputFilePath, tempOutputFilePath, compressionTemplates[templateIdx])
                if (compressedClipSize == 0) {
                    DLog.d(tag, "Video compression not possible for $inputFilePath")
                    return null  //Compression isn't possible, abort early
                }
                val clippedCompressionRatio = uncompressedClipSize / compressedClipSize.toFloat()
                DLog.i(tag,
                    "Video compression from $uncompressedClipSizeInt to $compressedClipSize bytes, command: [$templateIdx] ${compressionTemplates[templateIdx]}, compression: $clippedCompressionRatio")

                if (clippedCompressionRatio >= compressionRatio - 0.25f) {
                    return compressionTemplates[templateIdx]
                }

                if (clippedCompressionRatio > maxCompressionRatioSoFar) {
                    maxCompressionRatioSoFar = clippedCompressionRatio
                    bestTemplateIdx = templateIdx
                }
            }
        }
        catch (e: Exception) {
            DLog.e(tag, "Error during video compression in getCompressionTemplateForDesiredCompressionRatio", e)
        }

        return compressionTemplates[bestTemplateIdx]
    }

    private suspend fun compressClip(inputFilePath: String, outputFilePath: String, compressionTemplate: String): Int {
        try {
            return compressClipByTemplate(inputFilePath, outputFilePath, compressionTemplate, 5)
        }
        catch (e: Exception) {
//            DLog.e(tag, "Error during video compression in compressClip", e)
            return 0
        }
    }

    private suspend fun compressClipByTemplate(inputFilePath: String, outputFilePath: String, compressionTemplate: String, clipDurationSecs: Int): Int {
        if (compressInBackground(inputFilePath, outputFilePath, compressionTemplate, clipDurationSecs * 1024L)) {
            val newSize = File(outputFilePath).length()
            return newSize.toInt()
        } else {
            DLog.e(tag, "Video compression failed for $inputFilePath")
            return 0
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun compressInBackground(inputFilePath: String, outputFilePath: String, compressionCommand: String, clipDurationMs: Long = 0L): Boolean {
        var result = true

        if (File(outputFilePath).exists()) {
            File(outputFilePath).delete()
        }
        withContext(Dispatchers.Main) { // Transformer requires Dispatchers.Main
            suspendCancellableCoroutine { continuation ->
                try {
                    val videoInfo = videoInfo(inputFilePath)
                    val tokens = xlateDesiredCompressionCommand(compressionCommand, inputFilePath, outputFilePath, videoInfo).split("|")
                    val videoWidth = videoInfo[MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH]?.toIntOrNull() ?: 0
                    val videoHeight = videoInfo[MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT]?.toIntOrNull() ?: 0
                    val videoSizeScale = tokens[0].toFloat() / if (videoWidth > videoHeight) videoWidth.toFloat() else videoHeight.toFloat()
                    val frameRateFps = tokens[1].toFloat()
                    val convertToH265 = (tokens[2] == "T")
                    val editedMediaItem = createEditedMediaItem(inputFilePath, clipDurationMs, videoSizeScale, frameRateFps)  //Input transformations

                    //Process transformer to create new file
                    val transformerBuilder = Transformer.Builder(context)
                    if (convertToH265) {
                        with (transformerBuilder) {
                            transformerBuilder.setVideoMimeType(MimeTypes.VIDEO_H265)
                            transformerBuilder.setAudioMimeType(MimeTypes.AUDIO_AAC)
                        }
                    }
                    with (transformerBuilder) {
                        experimentalSetTrimOptimizationEnabled(true)
                        addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                continuation.resume(exportResult)
                            }

                            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                result = false
                                DLog.e(tag, "Video compression failed for '${inputFilePath}' with exportException: ${exportException.message}")
                                continuation.resumeWithException(exportException)
                            }
                        })
                    }
                    transformerBuilder.build().start(editedMediaItem, outputFilePath)
                }
                catch (e: Exception) {
                    DLog.e(tag, "Error during video compression: ${e.message}")
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
            DLog.e(tag, "Error retrieving video width: ${e.message}")
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
