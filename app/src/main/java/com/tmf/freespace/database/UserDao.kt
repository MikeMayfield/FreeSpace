package com.tmf.freespace.database

import com.tmf.freespace.models.CloudStorageType
import com.tmf.freespace.models.User
import java.util.UUID

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
    fun get(): User {
        database.read.rawQuery("SELECT * FROM User LIMIT 1", null).use { cursor ->
            if (cursor.moveToFirst()) {
                return User.fromCursor(cursor)!!
            } else {
                return User(0, UUID.randomUUID(), "", "", "", 0, CloudStorageType.Simulated)
            }
        }
    }
}