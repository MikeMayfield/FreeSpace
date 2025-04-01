package com.tmf.freespace.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import com.tmf.freespace.database.AppDatabase
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType

class MediaFileDao(private val database: AppDatabase) {
    private val tableName = "MediaFile"

    //Insert record if it doesn't already exist (based on MediaStoreID). Call with Async.Wait if new record ID is needed
    fun insertIfNew(mediaFile: MediaFile) {
        val newId = database.writable.insertWithOnConflict(tableName, null, getContentValues(mediaFile), CONFLICT_IGNORE)
        if (newId != -1L && newId != mediaFile.id) {
            mediaFile.id = newId  //Save new ID for record if inserted
        }
    }

    fun getFilesToBeCompressed() : Cursor {
        //SELECT * FROM MediaFile WHERE currentCompressionLevel != desiredCompressionLevel
        return database.readOnly.query(tableName, null, "currentCompressionLevel != desiredCompressionLevel", null, null, null, null)
    }

    fun nextMediaFile(cursor: Cursor) : MediaFile? {
        if (cursor.moveToNext()) {
            return MediaFile(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("mediaStoreID")),
                displayName = cursor.getString(cursor.getColumnIndexOrThrow("displayName")),
                directoryPath = cursor.getString(cursor.getColumnIndexOrThrow("relativePath")),
                originalSize = cursor.getInt(cursor.getColumnIndexOrThrow("originalSize")),
                compressedSize = cursor.getInt(cursor.getColumnIndexOrThrow("compressedSize")),
                width = cursor.getInt(cursor.getColumnIndexOrThrow("width")),
                height = cursor.getInt(cursor.getColumnIndexOrThrow("height")),
                mediaType = MediaType.entries[cursor.getInt(cursor.getColumnIndexOrThrow("mediaType"))],
                currentCompressionLevel = cursor.getInt(cursor.getColumnIndexOrThrow("currentCompressionLevel")),
                desiredCompressionLevel = cursor.getInt(cursor.getColumnIndexOrThrow("desiredCompressionLevel")),
                creationDtm = cursor.getLong(cursor.getColumnIndexOrThrow("creationDtm")),
                modifiedDtm = cursor.getLong(cursor.getColumnIndexOrThrow("modifiedDtm")),
                isOnServer = cursor.getInt(cursor.getColumnIndexOrThrow("isOnServer")) != 0,
            )
        }
        else {
            return null
        }
    }

    private fun getContentValues(mediaFile: MediaFile, excludeId: Boolean = false) : ContentValues {
        return ContentValues().apply {
            put("id", mediaFile.id)
            put("displayName", mediaFile.displayName)
            put("directoryPath", mediaFile.directoryPath)
            put("originalSize", mediaFile.originalSize)
            put("compressedSize", mediaFile.compressedSize)
            put("width", mediaFile.width)
            put("height", mediaFile.height)
            put("mediaType", mediaFile.mediaType.ordinal)
            put("currentCompressionLevel", mediaFile.currentCompressionLevel)
            put("desiredCompressionLevel", mediaFile.desiredCompressionLevel)
            put("creationDtm", mediaFile.creationDtm)
            put("modifiedDtm", mediaFile.modifiedDtm)
            put("isOnServer", database.boolToInt(mediaFile.isOnServer))
        }
    }
}