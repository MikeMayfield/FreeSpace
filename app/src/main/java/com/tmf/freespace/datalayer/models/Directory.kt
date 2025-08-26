package com.tmf.freespace.datalayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

//Directory portion of file path
@Entity()
data class Directory(
    val diskID: Int,
    val path: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
)
