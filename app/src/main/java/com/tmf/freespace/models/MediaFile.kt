package com.tmf.freespace.models

//Media File data
data class MediaFile(
    val id: Long = 0,  //Based on mediaStoreID
    val displayName: String,
    val relativePath: String,
//    val directoryPathID: Int,
    val originalSize: Int,
    var compressedSize: Int = originalSize,
    val width: Int,
    val height: Int,
    val mediaType: MediaType,
    var currentCompressionLevel: Int = 0,
    var desiredCompressionLevel: Int = 0,
    val creationDtm: Long,  //Seconds since 1970-01-01T00:00:00Z
    val modifiedDtm: Long,  //Seconds since 1970-01-01T00:00:00Z
    var isOnServer: Boolean,
)
