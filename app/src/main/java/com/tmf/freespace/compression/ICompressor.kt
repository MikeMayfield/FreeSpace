package com.tmf.freespace.compression

import android.content.Context
import com.tmf.freespace.models.MediaFile

abstract class ICompressor(val context: Context) {
    abstract val ffmpegCompressionCommands : List<String>

    //Compress media file using FFmpeg. Returns the compressed file size
    open fun compress(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String) : Boolean {
        val ffmpegCommand = ffmpegCommand(mediaFile, inputFilePath, outputFilePath)
        return if (ffmpegCommand.isNotEmpty()) ffmpeg.runCommand(ffmpegCommand) else false
    }

    fun ffmpegCommand(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String) : String {
        return ffmpegCompressionCommands[if (ffmpegCompressionCommands.size > mediaFile.desiredCompressionLevel) mediaFile.desiredCompressionLevel else 0]
            .replace("{{inputFilePath}}", inputFilePath)
            .replace("{{outputFilePath}}", outputFilePath)
    }

    companion object {
        val ffmpeg = FFmpeg()  //Shared FFmpeg instance for all compressors
    }
}