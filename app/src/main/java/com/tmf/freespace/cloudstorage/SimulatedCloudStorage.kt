package com.tmf.freespace.cloudstorage

import com.tmf.freespace.models.MediaFile

class SimulatedCloudStorage : ICloudStorage {
    override fun init() {
        //Nothing to do
    }

    override fun login(username: String, password: String): String {
        return "$username:$password"
    }

    override fun sendMediaFile(mediaFile: MediaFile) {
        TODO("Not yet implemented")
    }

    override fun restoreMediaFile(mediaFile: MediaFile) {
        TODO("Not yet implemented")
    }

}