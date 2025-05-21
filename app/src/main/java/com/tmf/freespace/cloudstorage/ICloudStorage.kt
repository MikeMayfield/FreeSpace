package com.tmf.freespace.cloudstorage

import android.content.Context
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User

interface ICloudStorage {
    fun init(user: User, context: Context) : String
    fun sendMediaFile(mediaFile: MediaFile, encoded: Boolean = false)  //TODO  Before uploading video, use ffmpeg -movflags +faststart option with FFmpeg can relocate the necessary metadata to the beginning, allowing file to be streamed, if sufficient bandwidth

    fun restoreMediaFile(mediaFile: MediaFile, encoded: Boolean = false) : String
}