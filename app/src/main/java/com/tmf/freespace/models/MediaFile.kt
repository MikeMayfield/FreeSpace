package com.tmf.freespace.models

import android.content.ContentValues
import android.database.Cursor
import java.nio.file.Paths
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


//Media File data
data class MediaFile @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Long = 0,  //Based on mediaStoreID
    val fullPath: String,
    val originalSize: Int,
    var compressedSize: Int = originalSize,
    val width: Int,
    val height: Int,
    val mediaType: MediaType,
    var currentCompressionLevel: Int = 0,
    var desiredCompressionLevel: Int = 0,
    val creationDtm: Long,  //Seconds since 1970-01-01T00:00:00Z
    val modifiedDtm: Long,  //Seconds since 1970-01-01T00:00:00Z
    var serverID: Int,
) {

    @OptIn(ExperimentalUuidApi::class)
    fun getContentValues(excludeId: Boolean = false) : ContentValues {
        return ContentValues().apply {
            put("id", id)
            put("fullPath", fullPath)
            put("originalSize", originalSize)
            put("compressedSize", compressedSize)
            put("width", width)
            put("height", height)
            put("mediaType", mediaType.ordinal)
            put("currentCompressionLevel", currentCompressionLevel)
            put("desiredCompressionLevel", desiredCompressionLevel)
            put("creationDtm", creationDtm)
            put("modifiedDtm", modifiedDtm)
            put("serverID", serverID)
        }
    }

    val fileType: String
        get() {
            val fileName = Paths.get(fullPath).fileName.toString()
            val lastDotIndex = fileName.lastIndexOf('.')

            return if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
                fileName.substring(lastDotIndex + 1).lowercase()
            } else {
                ""
            }
        }

    val displayName: String
        get() = Paths.get(fullPath).fileName.toString()

    val absolutePath: String
        get() = if (fullPath.contains(':')) fullPath.split(':')[1] else fullPath

    val isOnServer: Boolean
        get() = serverID >= 0

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun fromCursor(cursor: Cursor) : MediaFile? {
            if (cursor.moveToNext()) {
                return MediaFile(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    fullPath = cursor.getString(cursor.getColumnIndexOrThrow("fullPath")),
                    originalSize = cursor.getInt(cursor.getColumnIndexOrThrow("originalSize")),
                    compressedSize = cursor.getInt(cursor.getColumnIndexOrThrow("compressedSize")),
                    width = cursor.getInt(cursor.getColumnIndexOrThrow("width")),
                    height = cursor.getInt(cursor.getColumnIndexOrThrow("height")),
                    mediaType = MediaType.entries[cursor.getInt(cursor.getColumnIndexOrThrow("mediaType"))],
                    currentCompressionLevel = cursor.getInt(cursor.getColumnIndexOrThrow("currentCompressionLevel")),
                    desiredCompressionLevel = cursor.getInt(cursor.getColumnIndexOrThrow("desiredCompressionLevel")),
                    creationDtm = cursor.getLong(cursor.getColumnIndexOrThrow("creationDtm")),
                    modifiedDtm = cursor.getLong(cursor.getColumnIndexOrThrow("modifiedDtm")),
                    serverID = cursor.getInt(cursor.getColumnIndexOrThrow("serverID")),
                )
            }
            else {
                return null
            }
        }

        fun createTable() : String {
            val sb = StringBuilder("CREATE TABLE IF NOT EXISTS MediaFile (")
            sb.append("id INTEGER NOT NULL PRIMARY KEY, ")
            sb.append("fullPath TEXT NOT NULL, ")
            sb.append("originalSize INTEGER NOT NULL, ")
            sb.append("compressedSize INTEGER NOT NULL, ")
            sb.append("width INTEGER NOT NULL, ")
            sb.append("height INTEGER NOT NULL, ")
            sb.append("mediaType INTEGER NOT NULL, ")
            sb.append("currentCompressionLevel INTEGER NOT NULL, ")
            sb.append("desiredCompressionLevel INTEGER NOT NULL, ")
            sb.append("creationDtm INTEGER NOT NULL, ")
            sb.append("modifiedDtm INTEGER NOT NULL, ")
            sb.append("serverID INTEGER NULL ")
            sb.append(");")
            return sb.toString()
        }
    }
}
