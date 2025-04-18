package com.tmf.freespace.compression

import android.content.Context
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType

class Compressor(val context: Context) {
    private var imageCompressor: ImageCompressor = ImageCompressor(context)
    private var videoCompressor: VideoCompressor = VideoCompressor(context)
    private var audioCompressor: AudioCompressor = AudioCompressor(context)
    private val outputFilePath = "${context.getExternalFilesDir(null)?.absolutePath}/output."

    fun compress(mediaFile: MediaFile): Int {
        return when (mediaFile.mediaType) {
            MediaType.IMAGE -> {
                imageCompressor.compress(mediaFile, outputFilePath + "jpg")
            }

            MediaType.VIDEO -> {
                videoCompressor.compress(mediaFile, outputFilePath + "mp4")
            }

            MediaType.AUDIO -> {
                audioCompressor.compress(mediaFile, outputFilePath + "mp3")
            }
        }
    }
}