package com.tmf.freespace.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Create tables here
        db.execSQL(createDiskTable())
        db.execSQL(createDirectoryTable())
        db.execSQL(createMediaFileTable())
        db.execSQL(createUserTable())
        db.execSQL(createTeraBoxTable())
        db.execSQL(createPreferencesTable())
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

    private fun createDiskTable() : String {
        val sb = StringBuilder("CREATE TABLE IF NOT EXISTS Disk (")
        sb.append("id INTEGER PRIMARY KEY AUTOINCREMENT, ")
        sb.append("path TEXT NOT NULL, ")
        sb.append("expandedSizeBytes INTEGER NOT NULL")
        sb.append(");")

        return sb.toString()
    }

    private fun createDirectoryTable(): String {
        val sb = StringBuilder("CREATE TABLE IF NOT EXISTS Directory (")
        sb.append("id INTEGER PRIMARY KEY AUTOINCREMENT, ")
        sb.append("diskID INTEGER NOT NULL, ")
        sb.append("path TEXT NOT NULL")
        sb.append(");")

        return sb.toString()
    }

    private fun createMediaFileTable(): String {
        val sb = StringBuilder("CREATE TABLE IF NOT EXISTS MediaFile (")
        sb.append("id INTEGER NOT NULL PRIMARY KEY, ")
        sb.append("displayName TEXT NOT NULL, ")
        sb.append("relativePath TEXT NOT NULL, ")
        sb.append("originalSize INTEGER NOT NULL, ")
        sb.append("compressedSize INTEGER NOT NULL, ")
        sb.append("width INTEGER NOT NULL, ")
        sb.append("height INTEGER NOT NULL, ")
        sb.append("mediaType INTEGER NOT NULL, ")
        sb.append("currentCompressionLevel INTEGER NOT NULL, ")
        sb.append("desiredCompressionLevel INTEGER NOT NULL, ")
        sb.append("creationDtm INTEGER NOT NULL, ")
        sb.append("modifiedDtm INTEGER NOT NULL, ")
        sb.append("isOnServer INTEGER NOT NULL ")
        sb.append(");")

        return sb.toString()
    }

    private fun createUserTable(): String {
        val sb = StringBuilder("CREATE TABLE IF NOT EXISTS User (")
        sb.append("id INTEGER PRIMARY KEY AUTOINCREMENT, ")
        sb.append("idGuid TEXT NOT NULL, ")
        sb.append("phoneNumber TEXT NOT NULL, ")
        sb.append("emailAddress TEXT NOT NULL, ")
        sb.append("password TEXT NOT NULL, ")
        sb.append("maxExpansionAllowed INTEGER NOT NULL, ")
        sb.append("externalStorageType INTEGER NOT NULL")
        sb.append(");")

        return sb.toString()
    }

    private fun createTeraBoxTable(): String {
        val sb = StringBuilder("CREATE TABLE IF NOT EXISTS TeraBox (")
        sb.append("userName TEXT NOT NULL, ")
        sb.append("password TEXT NOT NULL, ")
        sb.append("spaceAvailableBytes INTEGER NOT NULL, ")
        sb.append("spaceUsedBytes INTEGER NOT NULL")
        sb.append(");")

        return sb.toString()
    }

    private fun createPreferencesTable(): String {
        return "CREATE TABLE IF NOT EXISTS Preferences (" +
                "desiredFreeSpaceGB INTEGER NOT NULL," +
                "shouldCompressImages INTEGER NOT NULL," +
                "shouldCompressVideos INTEGER NOT NULL," +
                "shouldCompressAudios INTEGER NOT NULL," +
                "shouldCompressDocuments INTEGER NOT NULL," +
                "shouldCompressOtherFiles INTEGER NOT NULL," +
                "requireWifi INTEGER NOT NULL," +
                "screenMustBeOff INTEGER NOT NULL," +
                "emailAddress TEXT NOT NULL," +
                "password TEXT NOT NULL" +
                ")"
    }

}