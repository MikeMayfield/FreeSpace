package com.tmf.freespace.cloudstorage

import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User

class SimulatedCloudStorage : ICloudStorage {
    override fun init(user: User): String {
        return "${user.emailAddress}_${user.password}"
    }

    override fun sendMediaFile(mediaFile: MediaFile) {
        TODO("Not yet implemented")
    }

    override fun restoreMediaFile(mediaFile: MediaFile) {
        TODO("Not yet implemented")
    }
}