package com.tmf.freespace.datalayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MediaFile(
    val mediaStoreID: Long,  //TODO BUG: **** MediaStore ID can change between reboots. Fix this!!
    val fullPath: String,
    val originalSize: Int,
    var compressedSize: Int = originalSize,
    val width: Int,
    val height: Int,
    val mediaType: MediaType,
    var currentCompressionLevel: Int = 0,
    var desiredCompressionLevel: Int = currentCompressionLevel,
    val creationDtm: Long,  //Seconds since 1970-01-01T00:00:00Z
    val modifiedDtm: Long,  //Seconds since 1970-01-01T00:00:00Z
    var serverID: Long = -1,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
{
    /**
     * Flag: The file is on the cloud server
     */
    val isOnServer: Boolean
        get() = (serverID > 0)
}