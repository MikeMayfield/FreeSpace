package com.tmf.freespace.compression

import android.content.Context
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType

class Compressor(val context: Context) {
    private var imageCompressor: ImageCompressor = ImageCompressor(context)
    private var videoCompressor: VideoCompressor = VideoCompressor(context)
    private var audioCompressor: AudioCompressor = AudioCompressor(context)

    fun compress(mediaFile: MediaFile): Int {
        return when (mediaFile.mediaType) {
            MediaType.IMAGE -> {
                imageCompressor.compress(mediaFile)
            }

            MediaType.VIDEO -> {
                videoCompressor.compress(mediaFile)
            }

            MediaType.AUDIO -> {
                audioCompressor.compress(mediaFile)
            }
        }
    }
}