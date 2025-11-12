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
        var fileHasBeenDeleted = true
        var fileToCompress = mediaFileDao.getFileToCompress()

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
                fileToCompress = mediaFileDao.getFileToCompress()
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
     *
     */
    suspend fun deleteFilesDeletedFromMediaStore() {
        mediaFileDao.deleteMediaFilesMarkedAsNotUpdated()
    }

    suspend fun getMediaFileByFullPath(fullPath: String) : MediaFile? {
        return mediaFileDao.getMediaFileByFullPath(fullPath)
    }

    suspend fun getBytesRecovered(): Long {
        return mediaFileDao.getBytesRecovered()
    }

    suspend fun getTotalUncompressedMediaSize(): Long {
        return mediaFileDao.getTotalUncompressedSize()
    }

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