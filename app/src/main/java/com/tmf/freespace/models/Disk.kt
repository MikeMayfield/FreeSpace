package com.tmf.freespace.models


//Disk portion of file path
data class Disk(
    var id: Int = 0,
    val path: String,  //Path to disk (the part before the directory path)
    val expandedSizeBytes: Long,  //Amount if disk space added by compression, in bytes
)
