package com.tmf.freespace.datalayer.datasources.local.dao

import User
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(user: User)

    @Query("SELECT * FROM User LIMIT 1")
    suspend fun get(): User

}