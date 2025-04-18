package com.tmf.freespace.compression

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

//Placeholder for FFmpeg library support. Replace with proper implementation when available
class FFmpeg() {
    fun runCommand(ffmpegCommand: String) : Boolean {
        val session = FFmpegKit.execute(ffmpegCommand)
        return ReturnCode.isSuccess(session.returnCode)
    }
}