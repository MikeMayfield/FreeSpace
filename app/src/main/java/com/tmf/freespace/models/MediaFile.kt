package com.tmf.freespace.models

import android.content.ContentValues
import android.database.Cursor

//Media File data
data class MediaFile(
    val id: Long = 0,  //Based on mediaStoreID
    val displayName: String,
    val relativePath: String,
    val originalSize: Int,
    var compressedSize: Int = originalSize,
    val width: Int,
    val height: Int,
    val mediaType: MediaType,
    var currentCompressionLevel: Int = 0,
    var desiredCompressionLevel: Int = 0,
    val creationDtm: Long,  //Seconds since 1970-01-01T00:00:00Z
    val modifiedDtm: Long,  //Seconds since 1970-01-01T00:00:00Z
    var isOnServer: Boolean,
) {
    fun getContentValues(excludeId: Boolean = false) : ContentValues {
        return ContentValues().apply {
            put("id", id)
            put("displayName", displayName)
            put("relativePath", relativePath)
            put("originalSize", originalSize)
            put("compressedSize", compressedSize)
            put("width", width)
            put("height", height)
            put("mediaType", mediaType.ordinal)
            put("currentCompressionLevel", currentCompressionLevel)
            put("desiredCompressionLevel", desiredCompressionLevel)
            put("creationDtm", creationDtm)
            put("modifiedDtm", modifiedDtm)
            put("isOnServer", if (isOnServer) 1 else 0)
        }
    }

    companion object {
        fun fromCursor(cursor: Cursor) : MediaFile? {
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
    }
}
