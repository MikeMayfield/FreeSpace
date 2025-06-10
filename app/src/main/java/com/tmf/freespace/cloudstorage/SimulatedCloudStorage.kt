package com.tmf.freespace.cloudstorage

import android.content.Context
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User

class SimulatedCloudStorage(val user: User, val context: Context) : ICloudStorage {
    override suspend fun sendMediaFile(mediaFile: MediaFile, encoded: Boolean) {
//        val sourceFile = File(mediaFile.fullPath)
//        val targetFile = File("${mediaFile.fullPath}.rmt")
//
//        if (!sourceFile.exists()) {
//            throw NoSuchFileException(sourceFile)
//        }
//
//        sourceFile.copyTo(targetFile, true)
    }

    override suspend fun restoreMediaFile(mediaFile: MediaFile, outputFilePath: String, encoded: Boolean): Boolean {
        return false  //TODO("Not yet implemented")
    }

    override suspend fun close() {
        //TODO("Not yet implemented")
    }
}