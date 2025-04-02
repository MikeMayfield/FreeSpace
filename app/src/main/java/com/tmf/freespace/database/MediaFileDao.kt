package com.tmf.freespace.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType

class MediaFileDao(private val database: AppDatabase) {
    private val tableName = "MediaFile"

    //Insert record if it doesn't already exist (based on MediaStoreID). Call with Async.Wait if new record ID is needed
    fun insertIfNew(mediaFile: MediaFile) {
        database.writable.insertWithOnConflict(tableName, null, getContentValues(mediaFile), CONFLICT_IGNORE)
    }

    fun setCompressionLevel(minDateMs: Long, maxDateMs: Long, imageCompressionLevel: Int, videoCompressionLevel: Int)  {
        database.writable.execSQL(
            "UPDATE MediaFile " +
                    "SET desiredCompressionLevel = $imageCompressionLevel " +
                    "WHERE creationDtm <= $minDateMs AND creationDtm > $maxDateMs AND mediaType = ${MediaType.IMAGE.ordinal} AND currentCompressionLevel != $imageCompressionLevel AND originalSize > 4095")

        database.writable.execSQL(
            "UPDATE MediaFile " +
                    "SET desiredCompressionLevel = $videoCompressionLevel " +
                    "WHERE creationDtm <= $minDateMs AND creationDtm > $maxDateMs AND mediaType = ${MediaType.VIDEO.ordinal} AND currentCompressionLevel != $videoCompressionLevel AND originalSize > 4095")
    }

    fun getFilesToBeCompressed() : Cursor {
        return database.readOnly.rawQuery(
            "SELECT * FROM MediaFile ",
//            "SELECT * FROM MediaFile " +
//                    "WHERE currentCompressionLevel != desiredCompressionLevel " +
//                    "ORDER BY desiredCompressionLevel DESC, creationDtm DESC",
            null)
    }

    fun nextMediaFile(cursor: Cursor) : MediaFile? {
        if (cursor.moveToNext()) {
            return MediaFile(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                displayName = cursor.getString(cursor.getColumnIndexOrThrow("displayName")),
                relativePath = cursor.getString(cursor.getColumnIndexOrThrow("relativePath")),
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
            put("relativePath", mediaFile.relativePath)
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

    fun update(file: MediaFile) {
        database.writable.update(tableName, getContentValues(file, true), "id = ?", arrayOf(file.id.toString()))
    }
}