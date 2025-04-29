package com.tmf.freespace.compression

import android.content.Context
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType

class Compressor(val context: Context) {
    private var imageCompressor: ImageCompressor = ImageCompressor(context)
    private var videoCompressor: VideoCompressor = VideoCompressor(context)
    private var audioCompressor: AudioCompressor = AudioCompressor(context)
    private val outputFilePath = "${context.cacheDir.absolutePath}/freespace/output."

    fun compress(mediaFile: MediaFile): String? {
        return when (mediaFile.mediaType) {
            MediaType.IMAGE -> {
                if (imageCompressor.compress(mediaFile, outputFilePath + "jpg")) outputFilePath + "jpg" else null
            }

            MediaType.VIDEO -> {
                if (videoCompressor.compress(mediaFile, outputFilePath + "mp4")) outputFilePath + "mp4" else null
            }

            MediaType.AUDIO -> {
                if (audioCompressor.compress(mediaFile, outputFilePath + "mp3")) outputFilePath + "mp3" else null
            }
        }
    }
}