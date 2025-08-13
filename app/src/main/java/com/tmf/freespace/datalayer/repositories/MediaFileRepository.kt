package com.tmf.freespace.datalayer.repositories

import android.content.Context
import android.net.Uri
import com.tmf.freespace.datalayer.datasources.database.AppDatabase
import com.tmf.freespace.models.MediaFile

class MediaFileRepository(context: Context) {
    private val mediaFileDao = AppDatabase(context).mediaFileDao

    suspend fun getMediaFileByID(mediaID: Long): MediaFile? {
        return mediaFileDao.getByID(mediaID)
    }

    fun addMediaFile(mediaFile: MediaFile) {
        mediaFileDao.insertIfNew(mediaFile)
    }

    /**
     * Update media file in database
     *
     * @param mediaFile Media file to update
     */
    suspend fun updateMediaFile(mediaFile: MediaFile) {
        mediaFileDao.update(mediaFile)
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
    suspend fun uploadFileToCloud(mediaFile: MediaFile): Uri? {
        TODO()
    }

    /**
     * Download uncompressed media file from cloud storage
     *
     * @param mediaFile Media file to download
     * @param outputFilePath Path to file to download to
     */
    suspend fun downloadFileFromCloud(mediaFile: MediaFile): Uri? {
        TODO()
    }
}