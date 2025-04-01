package com.tmf.freespace.database

import android.content.Context
import com.tmf.freespace.database.MediaFileDao
import android.database.sqlite.SQLiteDatabase as Database

class AppDatabase(
    context: Context
) {
    private val dbHelper = DatabaseHelper(context)
    val readOnly: Database
        get() {
            return dbHelper.readableDatabase
        }
    val writable: Database
        get() {
            return dbHelper.writableDatabase
        }

    val mediaFileDao: MediaFileDao
        get() {
            return MediaFileDao(this)
        }

    fun boolToInt(value: Boolean): Int {
        return if (value) 1 else 0
    }
}