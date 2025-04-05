package com.tmf.freespace.cloudstorage

import com.tmf.freespace.models.MediaFile

interface ICloudStorage {
    fun init()
    fun login(username: String, password: String) : String
    fun sendMediaFile(mediaFile: MediaFile)
    fun restoreMediaFile(mediaFile: MediaFile)
}