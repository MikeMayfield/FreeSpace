package com.tmf.freespace.domainlayer.compression

import android.content.Context
import com.tmf.freespace.datalayer.models.MediaFile

class VideoCompressor(context: Context) : ICompressor(context) {
    override val compressionCommands = listOf(
        "",  //0: No compression
        "-y -i {{INPUT_FILE_PATH}} {{OUTPUT_FILE_PATH}}.mp4",  //1: Compression low  //TODO Define real command
        "-y -i {{INPUT_FILE_PATH}} {{OUTPUT_FILE_PATH}}.mp4",  //2: Compression medium  //TODO Define real command
        "-y -i {{INPUT_FILE_PATH}} {{OUTPUT_FILE_PATH}}.mp4",  //3: Compression high  //TODO Define real command
        "-y -i {{INPUT_FILE_PATH}} {{OUTPUT_FILE_PATH}}.mp4",  //4: Compression very high  //TODO Define real command
        "-y -i {{INPUT_FILE_PATH}} {{OUTPUT_FILE_PATH}}.mp4",  //5: Compression ultra high  //TODO Define real command
    )

    //Compress media file using FFmpeg. Returns the compressed file size
    override fun compress(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String): Boolean {
        val ffmpegCommand = getDesiredCompressionCommand(mediaFile, inputFilePath, outputFilePath)
        return if (ffmpegCommand.isNotEmpty()) ffmpeg.runCommand(ffmpegCommand) else false
    }
}
