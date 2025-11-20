package com.tmf.freespace.datalayer.datasources.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.models.MediaType
import java.util.UUID

@Dao
interface MediaFileDao {
    /**
     * Set desired compression level for media files in database based on their creation date range
     *
     * <param>mostRecentDtm</param> - Most recent date in range to set compression level for
     * <param>oldestDtm</param> - Oldest date in range to set compression level for
     * <param>compressionLevel</param> - Compression level to set
     * <param>mediaType</param> - Media type to set compression level for
     */
    @Query(
        "UPDATE MediaFile SET desiredCompressionRatio = :compressionLevel" +
                " WHERE creationDtm > :oldestDtm AND creationDtm <= :mostRecentDtm AND mediaType = :mediaType AND currentCompressionRatio < :compressionLevel AND originalSize > 8192"
    )
    suspend fun setCompressionLevel(mostRecentDtm: Long, oldestDtm: Long, compressionLevel: Int, mediaType: MediaType)

    /**
     * Select the next file to compress. The file will have the highest desired compression level and the largest current (possibly compressed) size
     */
    @Query(
        "SELECT * FROM MediaFile" +
                " WHERE currentCompressionRatio != desiredCompressionRatio AND desiredCompressionRatio > 0 AND originalSize > 0" +
                " ORDER BY MediaType ASC, desiredCompressionRatio DESC, compressedSize DESC, creationDtm DESC" +
                " LIMIT 1"
    )
    suspend fun getFileToCompress(): MediaFile?

    /**
     * Get media file by ID
     *
     * <param>id</param> - ID of media file to get
     */
    @Query("SELECT * FROM MediaFile" +
            " WHERE mediaFileID = :mediaFileID")
    suspend fun getMediaFileByID(mediaFileID: UUID): MediaFile?

    /**
     * Update existing MediaFile record in database
     */
    @Update
    suspend fun updateMediaFile(mediaFile: MediaFile)

    /**
     * Delete media file from database
     */
    @Delete
    suspend fun deleteMediaFile(mediaFile: MediaFile)

    /**
     * Mark all media files as not updated yet (before processing all known media files in MediaStore)
     */
    @Query("UPDATE MediaFile SET mediaHasBeenUpdated = 0")
    suspend fun markAllMediaAsNotUpdated()

    /**
     * Add new media file to database or update MediaStoreID if it already exists
     */
    @Upsert
    suspend fun upsertMediaStoreID(mediaFile: MediaFile)

    /**
     * Delete all media files that were not been updated while processing all media files in MediaStore
     */
    @Query("DELETE FROM MediaFile WHERE mediaHasBeenUpdated = 0")
    suspend fun deleteMediaFilesMarkedAsNotUpdated() : Int

    /**
     * Get media file by full path
     *
     * <param>fullPath</param> - Full path of media file to get
     */
    @Query("SELECT * FROM MediaFile WHERE fullPath = :fullPath")
    suspend fun getMediaFileByFullPath(fullPath: String) : MediaFile?

    @Query("SELECT SUM(originalSize - compressedSize) FROM MediaFile")
    suspend fun getBytesRecovered(): Long

    @Query("SELECT SUM(originalSize) FROM MediaFile")
    suspend fun getTotalUncompressedSize(): Long

    @Query("SELECT SUM(compressedSize) FROM MediaFile")
    suspend fun getTotalCompressedMediaSize(): Long
}
