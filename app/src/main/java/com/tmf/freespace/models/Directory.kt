package com.tmf.freespace.models

//Directory portion of file path
data class Directory(
    var id: Int = 0,
    val diskID: Int,
    val path: String,
)
