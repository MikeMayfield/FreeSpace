package com.tmf.freespace.datalayer.repositories

import com.tmf.freespace.datalayer.datasources.local.database.AppDatabase
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.models.MediaType
import com.tmf.freespace.domainlayer.general.DLog
import java.io.File

class MediaFileRepository() {
    private val tag = "MediaFileRepository"

    private val mediaFileDao = AppDatabase.instance().mediaFileDao

    /**
     * Update media file in database
     *
     * @param mediaFile Media file to update
     */
    suspend fun updateMediaFile(mediaFile: MediaFile) {
        mediaFileDao.updateMediaFile(mediaFile)
    }

    /**
     * Set or update desired compression level for all image files in database based on their creation date
     */
    suspend fun setCompressionLevel(minAgeRangeDays: Int, maxAgeRangeDays: Int, compressionLevel: Int, mediaType: MediaType) {
        val nowSecs = System.currentTimeMillis() / 1_000L
        val secsPerDay: Long = 60L * 60L * 24L
        val mostRecentCreationDtm = nowSecs - minAgeRangeDays * secsPerDay
        val oldestCreationDtm = nowSecs - maxAgeRangeDays * secsPerDay

        mediaFileDao.setCompressionLevel(mostRecentCreationDtm, oldestCreationDtm, compressionLevel, mediaType)
    }

    /**
     * Find next file that needs to be compressed, select by highest compression level (desc), file size (desc)
     * If file no longer exists, remove from DB and try next file(s)
     */
    suspend fun getFileToCompress(): MediaFile? {
        //If in a trial subscription, only process older media to emphasize the quality of optimization while still recovering enough space to try to hit the 10MB goal
        val minCompressionRatio = 1//TODO if (PropertyBag.getString(SUBSCRIPTION_STATUS) == HomeScreenState.SubscriptionStatus.SUBSCRIBED.name) 1 else 3
        var fileHasBeenDeleted = true
        var fileToCompress = mediaFileDao.getFileToCompress(minCompressionRatio)

        while (fileHasBeenDeleted) {
            if (fileToCompress == null) {
                return null
            }

            /**
             * If media file has been deleted, remove it from the database
             */
            fileHasBeenDeleted = hasFileBeenDeleted(fileToCompress)
            if (fileHasBeenDeleted) {
                DLog.d(tag, "File ${fileToCompress.fullPath} has been deleted, removing from database")
                deleteFile(fileToCompress)
                fileToCompress = mediaFileDao.getFileToCompress(minCompressionRatio)
            }
        }

        return fileToCompress
    }

    /**
     * Delete media file from database
     */
    suspend fun deleteFile(fileToCompress: MediaFile) {
        mediaFileDao.deleteMediaFile(fileToCompress)
    }

    /**
     * Mark all media files as not updated yet
     */
    suspend fun markAllMediaAsNotUpdated() {
        mediaFileDao.markAllMediaAsNotUpdated()
    }

    /**
     * Insert or update media file in database
     *
     * @param mediaFile Media file to update
     */
    suspend fun upsertMediaFile(mediaFile: MediaFile) {
        mediaFileDao.upsertMediaStoreID(mediaFile)
    }

    /**
     *  Delete all files from DB that have been marked as not updated, since no longer exist on disk
     */
    suspend fun deleteFilesDeletedFromMediaStore() {
        mediaFileDao.deleteMediaFilesMarkedAsNotUpdated()
    }

    /**
     * Get media file by full path
     *
     * @param fullPath Full path of media file to get
     */
    suspend fun getMediaFileByFullPath(fullPath: String) : MediaFile? {
        return mediaFileDao.getMediaFileByFullPath(fullPath)
    }

    /**
     * Calculate total bytes recovered from compression
     *
     * @return Total bytes recovered from compression
     */
    suspend fun getBytesRecovered(): Long {
        return getTotalUncompressedMediaSize() - getTotalCompressedMediaSize()
    }

    /**
     * Get total uncompressed media size on bytes (bytes)
     */
    suspend fun getTotalUncompressedMediaSize(): Long {
        return mediaFileDao.getTotalUncompressedSize()
    }

    /**
     * Get total compressed media size on disk (bytes)
     */
    suspend fun getTotalCompressedMediaSize(): Long {
        return mediaFileDao.getTotalCompressedMediaSize()
    }

    /**
     * Check if media file has been deleted from MediaStore (and disk)
     *
     * @param fileToCompress Media file to check
     */
    private fun hasFileBeenDeleted(fileToCompress: MediaFile): Boolean {
        return !File(fileToCompress.fullPath).exists()
    }

}