package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.util.Log
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.models.MediaType
import java.io.File

class Compressor(val context: Context) {
    private val tag = Compressor::class.simpleName
    private val imageCompressor: ImageCompressor = ImageCompressor(context)
    private val videoCompressor: VideoCompressor = VideoCompressor(context)
    private val outputDirectoryPath = "${context.cacheDir.absolutePath}/freespace/"
    private val minFileSizeToCompress = 4 * 1024 * 2  //Don't compress if only slightly larger than disk cluster size. It won't actually save much/any physical space

    init {
        val outputDirectory = File(outputDirectoryPath)
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
    }

    /**
     * Compress a file to create a new file with the compressed data.
     *
     * @param mediaFile The MediaFile object representing the current media file
     * @param destinationFile The full path of the compressed file to create
     * @param compressionRatio Compression ratio (n:1)
     * @return The full path of the compressed file, or null if compression failed
     */
    fun compress(mediaFile: MediaFile, destinationFile: String, compressionRatio: Int): Boolean {
        val sourceFilePath = mediaFile.fullPath
        if (mediaFile.compressedSize > minFileSizeToCompress) {  //
            Log.d(tag, "File already compressed: $sourceFilePath")

            val mediaFileInfo = File(sourceFilePath)
            if (mediaFileInfo.exists()) {
                return when (mediaFile.mediaType) {
                    MediaType.IMAGE -> {
                        Log.d(tag, "Compressing image file: $sourceFilePath")
                        imageCompressor.compress(mediaFile, destinationFile, compressionRatio)
                    }

                    MediaType.VIDEO -> {
                        Log.d(tag, "Compressing video file: $sourceFilePath")
                        val success = videoCompressor.compress(mediaFile, destinationFile, compressionRatio)
                        Log.d(tag, "Compressed size: ${File(destinationFile).length()}")
                        success
                    }

//                    MediaType.AUDIO -> {
//                        true
//                    }
                }
            }
            Log.w(tag, "File not found: $sourceFilePath")
        }
        else {
            Log.d(tag, "File too small to compress: $sourceFilePath")
        }

        return false  //No compression occurred
    }
}
