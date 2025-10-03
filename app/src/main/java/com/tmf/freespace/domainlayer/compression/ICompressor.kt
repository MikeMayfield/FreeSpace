package com.tmf.freespace.domainlayer.compression

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.view.WindowManager
import com.tmf.freespace.datalayer.models.MediaFile

abstract class ICompressor(val context: Context) {
    abstract val compressionTemplates : List<String>

    /**
     * Compress media file using FFmpeg. Returns the compressed file size
     *
     * @param mediaFile The MediaFile object representing the current media file
     * @param outputFilePath The full path of the compressed file to create
     * @param compressionRatio Compression ratio (n:1)
     * @return Flag: File was compresses successfully
     */
    abstract fun compress(mediaFile: MediaFile, outputFilePath: String, compressionRatio: Int): Boolean


    fun xlateDesiredCompressionCommand(command: String, inputFilePath: String, outputFilePath: String, videoInfo: Map<Int, String?> = emptyMap()) : String {
        var xlatedCommand = command
        val screenWidthPixels = screenWidthPixels(context)
        if (xlatedCommand.isNotEmpty()) {
            if (xlatedCommand.contains("{{INPUT_FILE_PATH}}")) {
                xlatedCommand = xlatedCommand.replace("{{INPUT_FILE_PATH}}", inputFilePath)
            }

            if (xlatedCommand.contains("{{OUTPUT_FILE_PATH}}")) {
                xlatedCommand = xlatedCommand.replace("{{OUTPUT_FILE_PATH}}", outputFilePath)
            }

            val screenAt = xlatedCommand.indexOf("{{SCREEN_")
            if (screenAt >= 0) {
                var screenPct = xlatedCommand.substring(screenAt + 9, screenAt + 11).toInt()
                if (screenPct == 0) screenPct = 100
                xlatedCommand = xlatedCommand.substring(0, screenAt) + (screenWidthPixels * screenPct / 100).toString() + xlatedCommand.substring(screenAt + 13)
            }

            val videoAt = xlatedCommand.indexOf("{{VIDEO_")
            if (videoAt >= 0) {
                var videoPct = xlatedCommand.substring(videoAt + 8, videoAt + 10).toInt()
                if (videoPct == 0) videoPct = 100
                val videoWidth = videoInfo[MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH]?.toInt() ?: 0
                xlatedCommand = xlatedCommand.substring(0, videoAt) + (videoWidth * videoPct / 100).toString() + xlatedCommand.substring(videoAt + 12)
            }
        }

        return xlatedCommand
    }

    @Suppress("DEPRECATION")
    private fun screenWidthPixels(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val width = windowMetrics.bounds.width()
            val height = windowMetrics.bounds.height()
            if (width > height) width else height
        } else {
            val width = windowManager.defaultDisplay.width
            val height = windowManager.defaultDisplay.height
            if (width > height) width else height
        }
    }
}