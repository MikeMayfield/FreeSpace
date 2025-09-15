package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.util.Log
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.models.MediaType
import java.io.File

class Compressor(val context: Context) {
    private val TAG = Compressor::class.simpleName
    private val imageCompressor: ImageCompressor = ImageCompressor(context)
    private val videoCompressor: VideoCompressor = VideoCompressor(context)
    private val audioCompressor: AudioCompressor = AudioCompressor(context)
    private val outputDirectoryPath = "${context.cacheDir.absolutePath}/freespace/"
    private val minFileSizeToCompress = 4 * 1024 * 2  //Don't compress if barely larger than disk cluster size. It won't actually save much/any physical space

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
     * @param sourceFilePath The full path of the source file to compress
     * @param destinationFile The full path of the compressed file to create
     * @return The full path of the compressed file, or null if compression failed
     */
    fun compress(mediaFile: MediaFile, sourceFilePath: String, destinationFile: String): Boolean {
        if (mediaFile.compressedSize > minFileSizeToCompress) {  //
            val mediaFileInfo = File(sourceFilePath)
            if (mediaFileInfo.exists() && mediaFileInfo.length() > minFileSizeToCompress) {
                return when (mediaFile.mediaType) {
                    MediaType.IMAGE -> {
                        Log.v(TAG, "Compressing image file: $sourceFilePath")
                        imageCompressor.compress(mediaFile, sourceFilePath, destinationFile)
                    }

                    MediaType.VIDEO -> {
                        Log.v(TAG, "Compressing video file: $sourceFilePath")
                        videoCompressor.compress(mediaFile, sourceFilePath, destinationFile)
                    }

                    MediaType.AUDIO -> {
                        Log.v(TAG, "Compressing audio file: $sourceFilePath")
                        audioCompressor.compress(mediaFile, sourceFilePath, destinationFile)
                    }
                }
            }
        }

        return false  //No compression occurred
    }
}
