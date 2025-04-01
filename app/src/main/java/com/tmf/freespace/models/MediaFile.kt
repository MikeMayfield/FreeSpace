package com.tmf.freespace.models

//Media File data
data class MediaFile(
    var id: Long = 0,  //Based on mediaStoreID
    val displayName: String,
    val directoryPath: String,
//    val directoryPathID: Int,
    val originalSize: Int,
    val compressedSize: Int = originalSize,
    val width: Int,
    val height: Int,
    val mediaType: MediaType,
    val currentCompressionLevel: Int = 0,
    val desiredCompressionLevel: Int = 0,
    val creationDtm: Long,
    val modifiedDtm: Long,
    val isOnServer: Boolean,
)
