package com.tmf.freespace.datalayer.repositories

import android.content.Context
import com.tmf.freespace.datalayer.datasources.local.database.AppDatabase
import com.tmf.freespace.datalayer.models.FtpCredential
import com.tmf.freespace.datalayer.models.MediaFile

class MediaFileRepository(context: Context) {
     private val mediaFileDao = AppDatabase.create(context).mediaFileDao

    suspend fun getMediaFileByID(mediaID: Long): MediaFile? {
        return mediaFileDao.getMediaFileByID(mediaID)
    }

//    fun addMediaFile(mediaFile: MediaFile) {
//        mediaFileDao.insertIfNew(mediaFile)
//    }

    /**
     * Update media file in database
     *
     * @param mediaFile Media file to update
     */
    suspend fun updateMediaFile(mediaFile: MediaFile) {
        mediaFileDao.updateMediaFile(mediaFile)
    }

    /**
     * Find next file that needs to be compressed, select by highest compression level (desc), file size (desc)
     */
    fun getFileToCompress(): MediaFile? {
        TODO()
    }

    /**
     *Upload MediaStore file to cloud storage
     *
     * @param mediaFile Media file to upload
     * @param inputFilePath Path to file to upload
     */
    suspend fun uploadMediaToCloud(mediaFile: MediaFile, inputFilePath: String, ftpCredentials: FtpCredential): Boolean {
        TODO()
    }

    /**
     * Download uncompressed media file from cloud storage
     *
     * @param mediaFile Media file to download
     * @param outputFilePath Path to file to download to
     */
    suspend fun downloadMediaFromCloud(mediaFile: MediaFile, outputFilePath: String, ftpCredentials: FtpCredential): Boolean {
        TODO()
    }
}