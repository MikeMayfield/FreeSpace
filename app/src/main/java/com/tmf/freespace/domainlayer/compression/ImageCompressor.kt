package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.util.Log
import com.tmf.freespace.datalayer.models.MediaFile

class ImageCompressor(context: Context) : ICompressor(context) {
    //TODO Test ImageMagick vs ffmpeg for compression. ImageMagick is probably better for images, while ffmpeg is better for videos
    private val TAG = ImageCompressor::class.simpleName

    override val compressionCommands = listOf(
        //InputFilePath|OutputFilePath|ScreenWidth|CompressionRatio(originalSize/n)
        "",  //0: No compression
        "{{INPUT_FILE_PATH}}|{{OUTPUT_FILE_PATH}}|{{SCREEN_WIDTH}}|2",  //1: Compression low (31-60 days)  //TODO Define real command
        "{{INPUT_FILE_PATH}}|{{OUTPUT_FILE_PATH}}|{{SCREEN_WIDTH}}|3",  //2: Compression medium (61-180)  //TODO Define real command
        "{{INPUT_FILE_PATH}}|{{OUTPUT_FILE_PATH}}|{{SCREEN_WIDTH_33PCT}}|5",  //3: Compression high (181-365)  //TODO Define real command
        "{{INPUT_FILE_PATH}}|{{OUTPUT_FILE_PATH}}|{{SCREEN_WIDTH_33PCT}}|10",  //4: Compression very high (365+)  //TODO Define real command
    )

    //Compress an image file to JPEG
    /**
     * Compresses an image file to JPEG.
     *
     * @param mediaFile The MediaFile object representing the current media file
     * @param inputFilePath The full path of the source file to compress
     * @param outputFilePath The full path of the compressed file to create
     * @return True if compression was successful, false otherwise
     */
    override fun compress(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String): Boolean {
        val compressionCommand = getDesiredCompressionCommand(mediaFile, inputFilePath, outputFilePath)
        if (compressionCommand.isNotEmpty()) {
            val tokens = compressionCommand.split("|")
            if (tokens.size == 4) {
                Log.v(TAG, "Image compression command: $compressionCommand")
                val maxCompressedSize = mediaFile.originalSize / tokens[3].toInt()
                return CompressImageUtil().compressImage(tokens[0], tokens[1], tokens[2].toInt(), maxCompressedSize)
            } else {
                Log.e(TAG, "Invalid compression command; expected 4 tokens, got: $compressionCommand")
                return false
            }
        }

        return false  //No compression needed
    }

}
