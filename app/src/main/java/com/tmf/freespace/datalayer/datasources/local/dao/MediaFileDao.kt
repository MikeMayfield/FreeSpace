package com.tmf.freespace.datalayer.datasources.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.models.MediaType

@Dao
interface MediaFileDao {
    /**
     * Insert record if it doesn't already exist (based on MediaStoreID). Call with Async.Wait if new record ID is needed
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIfNew(mediaFile: MediaFile)

    /**
     * Set desired compression level for media files in database based on their creation date range
     *
     * <param>mostRecentDtm</param> - Most recent date in range to set compression level for
     * <param>oldestDtm</param> - Oldest date in range to set compression level for
     * <param>compressionLevel</param> - Compression level to set
     * <param>mediaType</param> - Media type to set compression level for
     */
    @Query(
        "UPDATE MediaFile SET desiredCompressionLevel = :compressionLevel" +
                " WHERE creationDtm > :oldestDtm AND creationDtm <= :mostRecentDtm AND mediaType = :mediaType AND currentCompressionLevel < :compressionLevel"
    )
    fun setCompressionLevel(mostRecentDtm: Long, oldestDtm: Long, compressionLevel: Int, mediaType: MediaType)

    /**
     * Select the next file to compress. The file will have the highest desired compression level and the largest current (possibly compressed) size
     */
    @Query(
        "SELECT * FROM MediaFile" +
                " WHERE currentCompressionLevel != desiredCompressionLevel AND desiredCompressionLevel > 0" +
                " ORDER BY desiredCompressionLevel DESC, compressedSize DESC, creationDtm DESC" +
                " LIMIT 1"
    )
    fun getFileToCompress(): MediaFile?

    /**
     * Get media file by ID
     *
     * <param>id</param> - ID of media file to get
     */
    @Query("SELECT * FROM MediaFile" +
            " WHERE id = :id")
    fun getMediaFileByID(id: Long): MediaFile?

    /**
     * Update existing MediaFile record in database
     */
    @Update
    fun updateMediaFile(mediaFile: MediaFile)

    /**
     * Delete media file from database
     */
    @Delete()
    fun deleteMediaFile(mediaFile: MediaFile)
}