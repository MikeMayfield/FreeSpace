package com.tmf.freespace.compression

import android.content.Context
import com.tmf.freespace.files.MediaStoreUtil
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType
import java.io.File

class Compressor(val context: Context) {
    private var imageCompressor: ImageCompressor = ImageCompressor(context)
    private var videoCompressor: VideoCompressor = VideoCompressor(context)
    private var audioCompressor: AudioCompressor = AudioCompressor(context)
    private val outputDirectoryPath = "${context.cacheDir.absolutePath}/freespace/"
    private val outputFilePath = "${outputDirectoryPath}compressed."
    private val mediaStoreUtil = MediaStoreUtil()

    init {
        val outputDirectory = File(outputDirectoryPath)
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
    }

    fun compress(mediaFile: MediaFile, extractedFilePath: String? = null): String? {
        val mediaFilePath = if (extractedFilePath.isNullOrEmpty()) mediaStoreUtil.extractFileFromMediaStore(context, mediaFile) else extractedFilePath
        if (mediaFilePath != null) {
            return when (mediaFile.mediaType) {
                MediaType.IMAGE -> {
                    if (imageCompressor.compress(mediaFile, mediaFilePath, outputFilePath + "jpg")) outputFilePath + "jpg" else null
                }

                MediaType.VIDEO -> {
                    if (videoCompressor.compress(mediaFile, mediaFilePath, outputFilePath + "mp4")) outputFilePath + "mp4" else null
                }

                MediaType.AUDIO -> {
                    if (audioCompressor.compress(mediaFile, mediaFilePath, outputFilePath + "mp3")) outputFilePath + "mp3" else null
                }
            }
        } else {
            return null
        }
    }
}