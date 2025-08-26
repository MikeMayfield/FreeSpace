package com.tmf.freespace.datalayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey


//Disk portion of local file path
@Entity()
data class Disk(
    val path: String,  //Path to disk (the part before the directory path)
    val expandedSizeBytes: Long,  //Amount if disk space added by compression, in bytes
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
)
