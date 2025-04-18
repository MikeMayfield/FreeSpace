package com.tmf.freespace.compression

import android.content.Context
import com.tmf.freespace.models.MediaFile
import java.io.File

abstract class ICompressor(val context: Context) {
    abstract val ffmpegCompressionCommands : List<String>

    //Compress media file using FFmpeg. Returns the compressed file size
    open fun compress(mediaFile: MediaFile, outputFilePath: String) : Int {
        val originalSize = mediaFile.compressedSize
        val ffmpegCommand = ffmpegCompressionCommands[if (ffmpegCompressionCommands.size > mediaFile.desiredCompressionLevel) mediaFile.desiredCompressionLevel else 0]
            .replace("{{inputFilePath}}", mediaFile.absolutePath)
            .replace("{{outputFilePath}}", outputFilePath)
        if (ffmpegCommand.isNotEmpty()) {
            if (ffmpeg.runCommand(ffmpegCommand)) {
                val compressedFile = File(outputFilePath)
                if (compressedFile.exists()) {
                    mediaFile.compressedSize = compressedFile.length().toInt()
                    return mediaFile.compressedSize
                }
            }
        }

        return originalSize  //No compression done. Return orginal (uncompressed) file size
    }

    companion object {
        val ffmpeg = FFmpeg()  //Shared FFmpeg instance for all compressors
    }
}