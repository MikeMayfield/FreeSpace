package com.tmf.freespace.datalayer.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "MediaFile",
    indices = [ Index(value = ["mediaFileID"], unique = true) ]
)
data class MediaFile(
    var mediaFileID: UUID = UUID.randomUUID(),
    val mediaStoreID: Long,
    @PrimaryKey val fullPath: String,
    val originalSize: Int,
    var compressedSize: Int = originalSize,
    val width: Int,
    val height: Int,
    var mediaType: MediaType,
    var currentCompressionRatio: Int,
    var desiredCompressionRatio: Int,
    val creationDtm: Long,  //milliseconds since 1970-01-01T00:00:00Z (Note: Not seconds like other DTMs)
    val modifiedDtm: Long,  //Seconds since 1970-01-01T00:00:00Z
    val dateInMediaStore: Long = 0L,  //Date/time added to MediaStore, in seconds since 1970-01-01T00:00:00Z
    var mediaHasBeenUpdated: Boolean = true,
)
