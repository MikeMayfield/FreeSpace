package com.tmf.freespace.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.Preferences
import com.tmf.freespace.models.User

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Create all tables
        db.execSQL(MediaFile.createTable())
        db.execSQL(User.createTable())
        db.execSQL(Preferences.createTable())
//        db.execSQL(TeraBox.createTable()))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades
        //TODO when upgrade support needed
        if (oldVersion < newVersion) {
            throw UnsupportedOperationException("Database upgrade not supported")
        }
    }

    companion object {
        private const val DATABASE_NAME = "FreeSpace.db"
        private const val DATABASE_VERSION = 1
    }
}