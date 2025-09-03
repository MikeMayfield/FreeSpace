package com.tmf.freespace.datalayer.repositories

import android.content.Context
import com.tmf.freespace.datalayer.datasources.cloudstorage.InterserverCloudStorage
import com.tmf.freespace.datalayer.datasources.local.database.AppDatabase
import com.tmf.freespace.datalayer.models.FtpCredential
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.models.MediaType

class MediaFileRepository(val context: Context) {
    private val mediaFileDao = AppDatabase.create(context).mediaFileDao

    fun getMediaFileByID(mediaID: Long): MediaFile? {
        return mediaFileDao.getMediaFileByID(mediaID)
    }

    /**
     * Add media file to database
     *
     * @param mediaFile Media file to add
     */
    fun addMediaFile(mediaFile: MediaFile) {
        mediaFileDao.insertIfNew(mediaFile)
    }

    /**
     * Update media file in database
     *
     * @param mediaFile Media file to update
     */
    fun updateMediaFile(mediaFile: MediaFile) {
        mediaFileDao.updateMediaFile(mediaFile)
    }

    /**
     * Set or update desired compression level for all image files in database based on their creation date
     */
    fun setCompressionLevel(minAgeRangeDays: Int, maxAgeRangeDays: Int, compressionLevel: Int, mediaType: MediaType) {
        val nowSecs = System.currentTimeMillis() / 1_000L
        val secsPerDay: Long = 60L * 60L * 24L
        val mostRecentCreationDtm = nowSecs - minAgeRangeDays * secsPerDay
        val oldestCreationDtm = nowSecs - maxAgeRangeDays * secsPerDay

        mediaFileDao.setCompressionLevel(mostRecentCreationDtm, oldestCreationDtm, compressionLevel, mediaType)
    }

    /**
     * Find next file that needs to be compressed, select by highest compression level (desc), file size (desc)
     */
    fun getFileToCompress(): MediaFile? {
        return mediaFileDao.getFileToCompress()
    }

    /**
     *Upload MediaStore file to cloud storage
     *
     * @param mediaFile Media file to upload
     * @param inputFilePath Path to file to upload
     */
    suspend fun uploadMediaToCloud(mediaFile: MediaFile, inputFilePath: String, ftpCredentials: FtpCredential): Boolean {
        val interserverCloudStorage = InterserverCloudStorage(UserRepository(context).getUser())
        return interserverCloudStorage.uploadMediaFile(mediaFile, inputFilePath, ftpCredentials)
    }

    /**
     * Download uncompressed media file from cloud storage
     *
     * @param mediaFile Media file to download
     * @param outputFilePath Path to file to download to
     */
    suspend fun downloadMediaFromCloud(mediaFile: MediaFile, outputFilePath: String, ftpCredentials: FtpCredential): Boolean {
        val interserverCloudStorage = InterserverCloudStorage(UserRepository(context).getUser())
        return interserverCloudStorage.downloadMediaFile(mediaFile, outputFilePath, ftpCredentials)
    }

    fun deleteFile(fileToCompress: MediaFile) {
        mediaFileDao.deleteMediaFile(fileToCompress)
    }
}