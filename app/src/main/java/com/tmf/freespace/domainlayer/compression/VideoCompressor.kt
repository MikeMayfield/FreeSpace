package com.tmf.freespace.domainlayer.compression

import android.content.Context
import com.tmf.freespace.datalayer.models.MediaFile

class VideoCompressor(context: Context) : ICompressor(context) {
    private val tag = VideoCompressor::class.simpleName

    override val compressionCommands = listOf(
        "",  //0: No compression
        "-y -i {{INPUT_FILE_PATH}} {{OUTPUT_FILE_PATH}}.mp4",  //1: Compression low
        "-y -i {{INPUT_FILE_PATH}} {{OUTPUT_FILE_PATH}}.mp4",  //2: Compression medium
        "-y -i {{INPUT_FILE_PATH}} {{OUTPUT_FILE_PATH}}.mp4",  //3: Compression high
        "-y -i {{INPUT_FILE_PATH}} {{OUTPUT_FILE_PATH}}.mp4",  //4: Compression very high
        "-y -i {{INPUT_FILE_PATH}} {{OUTPUT_FILE_PATH}}.mp4",  //5: Compression ultra high
    )

    //Compress media file using FFmpeg. Returns the compressed file size
    override fun compress(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String, maxCompressedSize: Int): Boolean {
        val ffmpegCommand = xlateDesiredCompressionCommand("TODO", inputFilePath, outputFilePath)
        return if (ffmpegCommand.isNotEmpty()) ffmpeg.runCommand(ffmpegCommand) else false
    }
}
