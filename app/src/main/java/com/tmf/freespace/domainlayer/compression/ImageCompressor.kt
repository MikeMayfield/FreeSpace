package com.tmf.freespace.domainlayer.compression

import android.content.Context
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.domainlayer.compression.ExifCopier.ExifCopier
import com.tmf.freespace.domainlayer.general.DLog
import java.io.File

class ImageCompressor(context: Context) : ICompressor(context) {
    private val tag = "ImageCompressor"

    override val compressionTemplates = listOf(
        //ScreenWidth|Quality(1..100, 1 lowest)
        "{{SCREEN_00}}|90",  //Compression very low
        "{{SCREEN_00}}|80",  //Compression low
        "{{SCREEN_00}}|60",  //Compression medium low
        "{{SCREEN_00}}|40",  //Compression medium
        "{{SCREEN_00}}|30",  //Compression medium high
        "{{SCREEN_00}}|50",  //Compression high
        "{{SCREEN_00}}|40",  //Compression higher
        "{{SCREEN_00}}|25",  //Compression very high
        "{{SCREEN_00}}|10",  //Compression very very high
    )

    //Compress an image file to JPEG
    /**
     * Compresses an image file to JPEG.
     *
     * @param mediaFile The MediaFile object representing the current media file
     * @param outputFilePath The full path of the compressed file to create
     * @param compressionRatio Compression ratio (n:1)
     * @return True if compression was successful, false otherwise
     */
    override fun compress(mediaFile: MediaFile, outputFilePath: String, compressionRatio: Int): Boolean {
        val inputFilePath = mediaFile.fullPath
        val maxCompressedSize = mediaFile.originalSize / compressionRatio

        for (compressionCommand in compressionTemplates) {
            val tokens = xlateDesiredCompressionCommand(compressionCommand, inputFilePath, outputFilePath).split("|")
            if (tokens.size == 2) {
                DLog.v(tag, "Image compression command: $compressionCommand")
                val desiredWidth = tokens[0].toInt()
                val quality = tokens[1].toInt()
                if (CompressImageUtil().compressImage(inputFilePath, outputFilePath, desiredWidth, quality)) {
                    if (File(outputFilePath).length() <= maxCompressedSize) {
                        return ExifCopier.copyExifData(inputFilePath, outputFilePath)
                    }
                } else {
                    DLog.e(tag, "Image compression failed for $inputFilePath")
                    return false
                }
            } else {
                DLog.e(tag, "Invalid compression command: $compressionCommand")
                return false
            }
        }

        return ExifCopier.copyExifData(inputFilePath, outputFilePath)  //Using maximum compression level
    }
}
