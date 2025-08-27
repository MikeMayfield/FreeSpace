package com.tmf.freespace.datalayer.datasources.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tmf.freespace.datalayer.models.MediaFile

@Dao
interface MediaFileDao {
    /**
     * Insert record if it doesn't already exist (based on MediaStoreID). Call with Async.Wait if new record ID is needed
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIfNew(mediaFile: MediaFile)

    @Query(
        "UPDATE MediaFile SET desiredCompressionLevel = :compressionLevel WHERE creationDtm <= :minDateMs AND creationDtm > :maxDateMs AND mediaType = :mediaType AND currentCompressionLevel < :compressionLevel"
    )
    fun setCompressionLevel(minDateMs: Long, maxDateMs: Long, compressionLevel: Int, mediaType: Int)

//    @Query("""
//        SELECT * FROM MediaFile
//            WHERE currentCompressionLevel != desiredCompressionLevel AND desiredCompressionLevel > 0
//            ORDER BY desiredCompressionLevel DESC, compressedSize DESC, creationDtm DESC LIMIT 1
//    """)
    @Query("""
        SELECT * FROM MediaFile 
    """)  //TODO Use code above after testing
    fun getFileToCompress() : MediaFile?

    /**
     * Get media file by ID
     */
    @Query("SELECT * FROM MediaFile WHERE id = :id")
    fun getMediaFileByID(id: Long): MediaFile?

    /**
     * Update existing MediaFile record in database
     */
    @Update
    fun updateMediaFile(mediaFile: MediaFile)

    /**
     * Update uncompressed media file to cloud server
     *
     * @param mediaFile MediaFile object to update
     * @param uncompressedFilePath Path to uncompressed file to upload
     * @return True if upload was successful
     */
    fun uploadFileToCloud(mediaFile: MediaFile, uncompressedFilePath: String): Boolean {
        return true  //TODO("Not yet implemented")
    }

    /**
     * Download saved uncompressed media file from cloud server
     *
     * @param mediaFile MediaFile object to update
     * @param uncompressedFilePath Path to uncompressed file to download
     * @return True if download was successful
     */
    fun downloadFileFromCloud(mediaFile: MediaFile, uncompressedFilePath: String): Boolean {
        TODO("Not yet implemented")
    }
}