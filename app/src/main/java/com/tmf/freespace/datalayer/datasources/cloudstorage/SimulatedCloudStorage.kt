package com.tmf.freespace.datalayer.datasources.cloudstorage

import android.content.Context
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User

class SimulatedCloudStorage(val user: User, val context: Context) : ICloudStorage {
    override suspend fun uploadMediaFile(mediaFile: MediaFile, encoded: Boolean) : String? {
//        val sourceFile = File(mediaFile.fullPath)
//        val targetFile = File("${mediaFile.fullPath}.rmt")
//
//        if (!sourceFile.exists()) {
//            throw NoSuchFileException(sourceFile)
//        }
//
//        sourceFile.copyTo(targetFile, true)
        return null  //TODO("Not yet implemented")

    }

    override suspend fun downloadMediaFile(mediaFile: MediaFile, outputFilePath: String, encoded: Boolean): Boolean {
        return false  //TODO("Not yet implemented")
    }

    override suspend fun close() {
        //TODO("Not yet implemented")
    }
}