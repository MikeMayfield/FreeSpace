package com.tmf.freespace.compression

import android.content.Context
import com.tmf.freespace.models.MediaFile

abstract class ICompressor(val context: Context) {
    abstract val ffmpegCompressionCommands : List<String>

    //Compress media file using FFmpeg
    fun compress(mediaFile: MediaFile) : Int {
        val ffmpegCommand = ffmpegCompressionCommands[if (ffmpegCompressionCommands.size > mediaFile.desiredCompressionLevel) mediaFile.desiredCompressionLevel else 0]
        if (ffmpegCommand.isNotEmpty()) {
            ffmpeg.runCommand(ffmpegCommand)
        }

        return (mediaFile.originalSize.toFloat() * 0.5f).toInt()  //TODO: Return (possibly) compressed file size
    }

    companion object {
        val ffmpeg = FFmpeg()  //Shared FFmpeg instance for all compressors
    }
}