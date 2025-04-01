package com.tmf.freespace.database

import com.tmf.freespace.models.Directory

class DirectoryDao {
    suspend fun insert(directory: Directory) {
        TODO("Not yet implemented")
    }

//    @Query("SELECT * FROM Directory")
//    suspend fun getAllDirectoryPathByID(): Map<@MapColumn(columnName = "id") Int, @MapColumn(columnName = "path") String>
}
