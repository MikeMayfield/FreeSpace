package com.tmf.freespace.compression

import android.content.Context
import com.tmf.freespace.models.MediaFile

class AudioCompressor(context: Context) : ICompressor(context) {
    override val ffmpegCompressionCommands = listOf(
        "",  //0: No compression
        "TODO",  //1: Compression low
        "TODO",  //2: Compression medium
        "TODO",  //3: Compression high
        "TODO",  //4: Compression very high
        "TODO",  //5: Compression ultra high

    )
}
