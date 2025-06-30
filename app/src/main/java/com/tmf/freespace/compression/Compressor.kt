package com.tmf.freespace.compression

import android.content.Context
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType
import java.io.File

class Compressor(val context: Context) {
    private var imageCompressor: ImageCompressor = ImageCompressor(context)
    private var videoCompressor: VideoCompressor = VideoCompressor(context)
    private var audioCompressor: AudioCompressor = AudioCompressor(context)
    private val outputDirectoryPath = "${context.cacheDir.absolutePath}/freespace/"
    val minFileSizeToCompress = 4 * 1024 * 2  //Don't compress if barely larger than disk cluster size. It won't actually save much/any physical space
    val minSignificantCompressionBytes = 128  //Minimum amount of compression that is worth processing  //TODO Make larger for production

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
                        imageCompressor.compress(mediaFile, sourceFilePath, destinationFile)
                    }

                    MediaType.VIDEO -> {
                        videoCompressor.compress(mediaFile, sourceFilePath, destinationFile)
                    }

                    MediaType.AUDIO -> {
                        audioCompressor.compress(mediaFile, sourceFilePath, destinationFile)
                    }
                }
            }
        }

        return false  //No compression occurred
    }
}