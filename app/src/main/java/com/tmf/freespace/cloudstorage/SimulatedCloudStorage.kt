package com.tmf.freespace.cloudstorage

import android.content.Context
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User
import java.io.File
import java.nio.file.Files

class SimulatedCloudStorage : ICloudStorage {
    private lateinit var user: User
    private lateinit var context: Context

    override fun init(user: User, context: Context): String {
        this.user = user
        this.context = context

        return "${user.emailAddress}_${user.password}"
    }

    override fun sendMediaFile(mediaFile: MediaFile, encoded: Boolean) {
//        val sourceFile = File(mediaFile.fullPath)
//        val targetFile = File("${mediaFile.fullPath}.rmt")
//
//        if (!sourceFile.exists()) {
//            throw NoSuchFileException(sourceFile)
//        }
//
//        sourceFile.copyTo(targetFile, true)
    }

    override fun restoreMediaFile(mediaFile: MediaFile, encoded: Boolean) {
//        TODO("Not yet implemented")
    }
}