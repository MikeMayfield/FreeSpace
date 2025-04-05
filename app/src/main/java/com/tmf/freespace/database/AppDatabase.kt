package com.tmf.freespace.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase as Database

class AppDatabase(
    context: Context
) {
    private val dbHelper = DatabaseHelper(context)
    val read: Database
        get() {
            return dbHelper.readableDatabase
        }
    val write: Database
        get() {
            return dbHelper.writableDatabase
        }

    val mediaFileDao: MediaFileDao
        get() {
            return MediaFileDao(this)
        }

    val userDao: UserDao
        get() {
            return UserDao(this)
        }

    fun boolToInt(value: Boolean): Int {
        return if (value) 1 else 0
    }
}