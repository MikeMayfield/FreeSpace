package com.tmf.freespace.cloudstorage

import android.content.Context
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User

interface ICloudStorage {
    fun init(user: User, context: Context) : String
    fun sendMediaFile(mediaFile: MediaFile, encoded: Boolean = false)
    fun restoreMediaFile(mediaFile: MediaFile, encoded: Boolean = false)
}