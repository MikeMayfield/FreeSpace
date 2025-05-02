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
    val minFileSizeToCompress = 4 * 1024 * 2  //Don't compress if barely larger than disk cluster size. It won't actually save much/any physical space

    init {
        val outputDirectory = File(outputDirectoryPath)
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
    }

    fun compress(mediaFile: MediaFile, extractedFilePath: String? = null): String? {
        if (mediaFile.compressedSize > minFileSizeToCompress) {  //
            val tempMediaFilePath = if (extractedFilePath.isNullOrEmpty()) mediaStoreUtil.extractFileFromMediaStore(context, mediaFile) else extractedFilePath
            if (tempMediaFilePath != null) {
                val mediaFileInfo = File(tempMediaFilePath)
                if (mediaFileInfo.exists() && mediaFileInfo.length() > minFileSizeToCompress) {
                    val compressedFilePath = when (mediaFile.mediaType) {
                        MediaType.IMAGE -> {
                            if (imageCompressor.compress(mediaFile, tempMediaFilePath, outputFilePath + "jpg")) outputFilePath + "jpg" else null
                        }

                        MediaType.VIDEO -> {
                            if (videoCompressor.compress(mediaFile, tempMediaFilePath, outputFilePath + "mp4")) outputFilePath + "mp4" else null
                        }

                        MediaType.AUDIO -> {
                            if (audioCompressor.compress(mediaFile, tempMediaFilePath, outputFilePath + "mp3")) outputFilePath + "mp3" else null
                        }
                    }

                    if (compressedFilePath != null) {
                        File(tempMediaFilePath).delete()
                        return compressedFilePath
                    }
                }
            }
        }

        return null
    }
}