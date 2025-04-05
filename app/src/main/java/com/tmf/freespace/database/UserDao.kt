package com.tmf.freespace.database

import android.database.sqlite.SQLiteDatabase
import com.tmf.freespace.models.User

class UserDao(private val database: AppDatabase) {
    private val tableName = "User"

//    fun insert(user: User) {
//        if (user.id == 0) {
//            val id = database.writable.insertWithOnConflict(tableName, null, getContentValues(user), SQLiteDatabase.CONFLICT_IGNORE)
//            user.id = id.toInt()
//        } else {
//            database.writable.update(tableName, getContentValues(user), "id = ?", arrayOf(user.id.toString()))
//        }
//
//    }

    //    @Query("SELECT * FROM User LIMIT 1")
    suspend fun get(): User {
        TODO("Not yet implemented")
    }

//    @Insert
//    suspend fun Insert(user: User)
}