package com.tmf.freespace.compression

import android.content.Context
import com.tmf.freespace.models.MediaFile

class VideoCompressor(context: Context) : ICompressor(context) {
    override val ffmpegCompressionCommands = listOf(
        "",  //0: No compression
        "-y -i {{inputFilePath}} {{outputFilePath}}",  //1: Compression low  //TODO Define real command
        "-y -i {{inputFilePath}} {{outputFilePath}}",  //2: Compression medium  //TODO Define real command
        "-y -i {{inputFilePath}} {{outputFilePath}}",  //3: Compression high  //TODO Define real command
        "-y -i {{inputFilePath}} {{outputFilePath}}",  //4: Compression very high  //TODO Define real command
        "-y -i {{inputFilePath}} {{outputFilePath}}",  //5: Compression ultra high  //TODO Define real command
    )
}
