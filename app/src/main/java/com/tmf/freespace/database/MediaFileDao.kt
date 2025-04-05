package com.tmf.freespace.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType

class MediaFileDao(private val database: AppDatabase) {
    private val tableName = "MediaFile"

    //Insert record if it doesn't already exist (based on MediaStoreID). Call with Async.Wait if new record ID is needed
    fun insertIfNew(mediaFile: MediaFile) {
        database.writable.insertWithOnConflict(tableName, null, mediaFile.getContentValues(), CONFLICT_IGNORE)
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
            "SELECT * FROM MediaFile " +
                    "WHERE currentCompressionLevel != desiredCompressionLevel " +
                    "ORDER BY desiredCompressionLevel DESC, creationDtm DESC",
            null)
    }

    fun nextMediaFile(cursor: Cursor) : MediaFile? {
        return MediaFile.fromCursor(cursor)
    }

    fun update(mediaFile: MediaFile) {
        database.writable.update(tableName, mediaFile.getContentValues(true), "id = ?", arrayOf(mediaFile.id.toString()))
    }
}