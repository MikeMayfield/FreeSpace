package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.WindowManager
import com.tmf.freespace.datalayer.models.MediaFile

class ImageCompressor(context: Context) : ICompressor(context) {
    //TODO Test ImageMagick vs ffmpeg for compression. ImageMagick is probably better for images, while ffmpeg is better for videos
    private val TAG = ImageCompressor::class.simpleName

    override val ffmpegCompressionCommands = listOf(
        "",  //0: No compression
        "-y -i {{INPUT_FILE_PATH}} -q:v 2 {{OUTPUT_FILE_PATH}}.jpg",  //1: Compression low  //TODO Define real command
        "-y -i {{INPUT_FILE_PATH}} -q:v 2 {{OUTPUT_FILE_PATH}}",  //2: Compression medium  //TODO Define real command
        "-y -i {{INPUT_FILE_PATH}} -q:v 20 {{OUTPUT_FILE_PATH}}",  //3: Compression high  //TODO Define real command
        "-y -i {{INPUT_FILE_PATH}} -q:v 20 {{OUTPUT_FILE_PATH}}",  //4: Compression very high  //TODO Define real command
        "-y -i {{INPUT_FILE_PATH}} -q:v 31 {{OUTPUT_FILE_PATH}}",  //5: Compression ultra high  //TODO Define real command
    )

    //Compress an image file to JPEG
    /**
     * Compresses an image file to JPEG.
     *
     * @param mediaFile The MediaFile object representing the current media file
     * @param inputFilePath The full path of the source file to compress
     * @param outputFilePath The full path of the compressed file to create
     * @return True if compression was successful, false otherwise
     */
    override fun compress(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String): Boolean {
        var ffmpegCommand = ffmpegCommand(mediaFile, inputFilePath, outputFilePath)
        if (ffmpegCommand.isNotEmpty()) {
            if (ffmpegCommand.contains("{{SCREEN_WIDTH}}")) {
                ffmpegCommand = ffmpegCommand.replace("{{SCREEN_WIDTH}}", screenWidthPixels(context).toString())
            }
            if (ffmpegCommand.contains("{{SCREEN_HEIGHT}}")) {
                ffmpegCommand = ffmpegCommand.replace("{{SCREEN_HEIGHT}}", screenHeightPixels(context).toString())
            }
            if (ffmpegCommand.contains("{{SCREEN_WIDTH_33PCT}}")) {
                ffmpegCommand = ffmpegCommand.replace("{{SCREEN_WIDTH_33PCT}}", (screenWidthPixels(context) * 0.33).toInt().toString())
            }
            if (ffmpegCommand.contains("{{SCREEN_HEIGHT_33PCT}}")) {
                ffmpegCommand = ffmpegCommand.replace("{{SCREEN_HEIGHT_33PCT}}", (screenHeightPixels(context) * 0.33).toInt().toString())
            }
            Log.v(TAG, "FFmpeg command: $ffmpegCommand")
            return ffmpeg.runCommand(ffmpegCommand)
        }

        return false  //No compression needed
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

    @Suppress("DEPRECATION")
    private fun screenHeightPixels(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            windowMetrics.bounds.height()
        } else {
            windowManager.defaultDisplay.height
        }
    }
}
