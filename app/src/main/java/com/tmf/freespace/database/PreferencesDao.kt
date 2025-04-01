package com.tmf.freespace.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType
import com.tmf.freespace.models.Preferences

class PreferencesDao(private val database: AppDatabase) {
    private val tableName = "Preferences"

    //Get the current preferences (create new first record if not yet saved)
    fun get(cursor: Cursor) : Preferences? {
        if (cursor.moveToNext()) {
            return Preferences(
                desiredFreeSpaceGB = cursor.getInt(cursor.getColumnIndexOrThrow("desiredFreeSpaceGB")),
                shouldCompressImages = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCompressImages")) != 0,
                shouldCompressVideos = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCompressVideos")) != 0,
                shouldCompressAudios = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCompressAudios")) != 0,
                shouldCompressDocuments = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCompressDocuments")) != 0,
                shouldCompressOtherFiles = cursor.getInt(cursor.getColumnIndexOrThrow("shouldCompressOtherFiles")) != 0,
                requireWifi = cursor.getInt(cursor.getColumnIndexOrThrow("requireWifi")) != 0,
                screenMustBeOff = cursor.getInt(cursor.getColumnIndexOrThrow("screenMustBeOff")) != 0,
                emailAddress = cursor.getString(cursor.getColumnIndexOrThrow("emailAddress")),
                password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
            )
        }
        else {
            val newPreferences = Preferences()
            database.writable.insertWithOnConflict(tableName, null, getContentValues(newPreferences), CONFLICT_IGNORE)
            return newPreferences
        }
    }

    private fun getContentValues(preferences: Preferences) : ContentValues {
        return ContentValues().apply {
            put("desiredFreeSpaceGB", preferences.desiredFreeSpaceGB)
            put("shouldCompressImages", database.boolToInt(preferences.shouldCompressImages))
            put("shouldCompressVideos", database.boolToInt(preferences.shouldCompressVideos))
            put("shouldCompressAudios", database.boolToInt(preferences.shouldCompressAudios))
            put("shouldCompressDocuments", database.boolToInt(preferences.shouldCompressDocuments))
            put("shouldCompressOtherFiles", database.boolToInt(preferences.shouldCompressOtherFiles))
            put("requireWifi", database.boolToInt(preferences.requireWifi))
            put("screenMustBeOff", database.boolToInt(preferences.screenMustBeOff))
            put("emailAddress", preferences.emailAddress)
            put("password", preferences.password)
        }
    }


}