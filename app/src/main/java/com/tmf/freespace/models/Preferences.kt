package com.tmf.freespace.models

import android.content.ContentValues
import android.database.Cursor
import java.util.UUID

data class Preferences(
    var desiredFreeSpaceGB: Int = 10,
    var shouldCompressImages: Boolean = true,
    var shouldCompressVideos: Boolean = true,
    var shouldCompressAudios: Boolean = true,
    var shouldCompressDocuments: Boolean = true,
    var shouldCompressOtherFiles: Boolean = true,
    var backupAllFiles: Boolean = false,
    var requireWifi: Boolean = true,
    var screenMustBeOff: Boolean = true,
    var emailAddress: String = "",
    var password: String = "",
) {
    fun getContentValues(excludeId: Boolean = false) : ContentValues {
        return ContentValues().apply {
            put("desiredFreeSpaceGB", desiredFreeSpaceGB)
            put("shouldCompressImages", shouldCompressImages)
            put("shouldCompressVideos", shouldCompressVideos)
            put("shouldCompressAudios", shouldCompressAudios)
            put("shouldCompressDocuments", shouldCompressDocuments)
            put("shouldCompressOtherFiles", shouldCompressOtherFiles)
            put("backupAllFiles", backupAllFiles)
            put("requireWifi", requireWifi)
            put("screenMustBeOff", screenMustBeOff)
            put("emailAddress", emailAddress)
            put("password", password)
        }
    }


    companion object {
        fun fromCursor(cursor: Cursor) : Preferences? {
            if (cursor.moveToNext()) {
                return Preferences(
                    desiredFreeSpaceGB = 1000_000_000, //TODO cursor.getInt(cursor.getColumnIndexOrThrow("desiredFreeSpaceGB")),
                    shouldCompressImages = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCompressImages")) != 0,
                    shouldCompressVideos = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCompressVideos")) != 0,
                    shouldCompressAudios = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCompressAudios")) != 0,
                    shouldCompressDocuments = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCompressDocuments")) != 0,
                    shouldCompressOtherFiles = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCompressOtherFiles")) != 0,
                    backupAllFiles = cursor.getInt(cursor.getColumnIndexOrThrow("backupAllFiles")) != 0,
                    requireWifi = cursor.getInt(cursor.getColumnIndexOrThrow("requireWifi")) != 0,
                    screenMustBeOff = cursor.getInt(cursor.getColumnIndexOrThrow("screenMustBeOff")) != 0,
                    emailAddress = cursor.getString(cursor.getColumnIndexOrThrow("emailAddress")),
                    password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                )
            }
            else {
                return null
            }
        }

        fun createTable() : String {
            val sb = StringBuilder("CREATE TABLE IF NOT EXISTS Preferences (")
            sb.append("id INTEGER PRIMARY KEY AUTOINCREMENT, ")
            sb.append("shouldCompressImages INTEGER NOT NULL, ")
            sb.append("shouldCompressVideos INTEGER NOT NULL, ")
            sb.append("shouldCompressAudios INTEGER NOT NULL, ")
            sb.append("shouldCompressDocuments INTEGER NOT NULL, ")
            sb.append("shouldCompressOtherFiles INTEGER NOT NULL, ")
            sb.append("backupAllFiles INTEGER NOT NULL, ")
            sb.append("requireWifi INTEGER NOT NULL, ")
            sb.append("screenMustBeOff INTEGER NOT NULL, ")
            sb.append("emailAddress TEXT NOT NULL, ")
            sb.append("password TEXT NOT NULL")
            sb.append(");")
            return sb.toString()
        }
    }

}
