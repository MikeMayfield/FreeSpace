package com.tmf.freespace.cloudstorage

import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User

interface ICloudStorage {
    fun init(user: User) : String
    fun sendMediaFile(mediaFile: MediaFile)
    fun restoreMediaFile(mediaFile: MediaFile)
}