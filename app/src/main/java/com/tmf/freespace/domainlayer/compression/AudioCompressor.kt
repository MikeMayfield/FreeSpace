package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.net.Uri
import com.tmf.freespace.models.MediaFile

class AudioCompressor(context: Context) : ICompressor(context) {
    override val ffmpegCompressionCommands = listOf(
        "",  //0: No compression
        "-y -i {{inputFilePath}} {{outputFilePath}}",  //1: Compression low  //TODO Define real command
        "-y -i {{inputFilePath}} {{outputFilePath}}",  //2: Compression medium  //TODO Define real command
        "-y -i {{inputFilePath}} {{outputFilePath}}",  //3: Compression high  //TODO Define real command
        "-y -i {{inputFilePath}} {{outputFilePath}}",  //4: Compression very high  //TODO Define real command
        "-y -i {{inputFilePath}} {{outputFilePath}}",  //5: Compression ultra high  //TODO Define real command
    )

    override fun compress(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String): Boolean {
        TODO("Implement this")
    }
}
