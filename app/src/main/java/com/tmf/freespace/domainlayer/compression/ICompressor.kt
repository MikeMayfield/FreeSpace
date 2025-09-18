package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.tmf.freespace.datalayer.models.MediaFile

abstract class ICompressor(val context: Context) {
    abstract val compressionCommands : List<String>

    /**
     * Compress media file using FFmpeg. Returns the compressed file size
     *
     * @param mediaFile The MediaFile object representing the current media file
     * @param inputFilePath The full path of the source file to compress
     * @param outputFilePath The full path of the compressed file to create
     * @param maxCompressedSize The maximum size of the compressed file
     * @return Flag: File was compresses successfully
     */
    abstract fun compress(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String, maxCompressedSize: Int) : Boolean


    fun xlateDesiredCompressionCommand(command: String, inputFilePath: String, outputFilePath: String) : String {
        var xlatedCommand = command
        val screenWidthPixels = screenWidthPixels(context)
        if (xlatedCommand.isNotEmpty()) {
            if (xlatedCommand.contains("{{INPUT_FILE_PATH}}")) {
                xlatedCommand = xlatedCommand.replace("{{INPUT_FILE_PATH}}", inputFilePath)
            }
            if (xlatedCommand.contains("{{OUTPUT_FILE_PATH}}")) {
                xlatedCommand = xlatedCommand.replace("{{OUTPUT_FILE_PATH}}", outputFilePath)
            }
            if (xlatedCommand.contains("{{SCREEN_WIDTH}}")) {
                xlatedCommand = xlatedCommand.replace("{{SCREEN_WIDTH}}", screenWidthPixels.toString())
            }
            if (xlatedCommand.contains("{{SCREEN_WIDTH_33PCT}}")) {
                xlatedCommand = xlatedCommand.replace("{{SCREEN_WIDTH_33PCT}}", (screenWidthPixels * 0.33).toInt().toString())
            }
            if (xlatedCommand.contains("{{SCREEN_WIDTH_25PCT}}")) {
                xlatedCommand = xlatedCommand.replace("{{SCREEN_WIDTH_25PCT}}", (screenWidthPixels * 0.25).toInt().toString())
            }
        }

        return xlatedCommand
    }

    @Suppress("DEPRECATION")
    private fun screenWidthPixels(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            windowMetrics.bounds.width()
        } else {
            windowManager.defaultDisplay.width
        }
    }

    companion object {
        val ffmpeg = FFmpeg()  //Shared FFmpeg instance for all compressors
    }
}