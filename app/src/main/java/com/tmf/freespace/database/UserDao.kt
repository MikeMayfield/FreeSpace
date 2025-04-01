package com.tmf.freespace.database

import com.tmf.freespace.models.User

interface UserDao {
    //    @Query("SELECT * FROM User LIMIT 1")
    suspend fun getData(): User {
        TODO("Not yet implemented")
    }

//    @Insert
//    suspend fun Insert(user: User)
}