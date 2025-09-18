package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.util.Log
import com.tmf.freespace.datalayer.models.MediaFile
import java.io.File

class ImageCompressor(context: Context) : ICompressor(context) {
    private val tag = ImageCompressor::class.simpleName

    override val compressionCommands = listOf(
        //InputFilePath|OutputFilePath|ScreenWidth|CompressionQuality(1..100, 1 lowest)
        "{{SCREEN_WIDTH}}|80",  //Compression low
        "{{SCREEN_WIDTH}}|50",  //Compression medium
        "{{SCREEN_WIDTH_33PCT}}|40",  //Compression high
        "{{SCREEN_WIDTH_25PCT}}|20",  //Compression very high
        "{{SCREEN_WIDTH_25PCT}}|10",  //Compression very very high
        "{{SCREEN_WIDTH_25PCT}}|5",  //Compression extremely high
    )

    //Compress an image file to JPEG
    /**
     * Compresses an image file to JPEG.
     *
     * @param mediaFile The MediaFile object representing the current media file
     * @param inputFilePath The full path of the source file to compress
     * @param outputFilePath The full path of the compressed file to create
     * @param maxCompressedSize The maximum size of the compressed file
     * @return True if compression was successful, false otherwise
     */
    override fun compress(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String, maxCompressedSize: Int): Boolean {
        for (compressionCommand in compressionCommands) {
            val tokens = xlateDesiredCompressionCommand(compressionCommand, inputFilePath, outputFilePath).split("|")
            if (tokens.size == 2) {
                Log.v(tag, "Image compression command: $compressionCommand")
                val desiredWidth = tokens[0].toInt()
                val quality = tokens[1].toInt()
                if (CompressImageUtil().compressImage(inputFilePath, outputFilePath, desiredWidth, quality)) {
                    if (File(outputFilePath).length() <= maxCompressedSize) {
                        return true
                    }
                } else {
                    Log.e(tag, "Image compression failed for $inputFilePath")
                    return false
                }
            } else {
                Log.e(tag, "Invalid compression command: $compressionCommand")
                return false
            }
        }

        return true  //Using maximum compression level
    }

}
