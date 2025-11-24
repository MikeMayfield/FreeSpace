package com.tmf.freespace.datalayer.mediastore

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.domainlayer.general.DLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MediaStoreUtil {
    val tag = "MediaStoreUtil"

    //region Public methods

    /**
     * Overwrite (i.e. replace) a file in the MediaStore with a different file.
     *
     * @param context The application context.
     * @param mediaFile Media that you want to replace in the MediaStore.
     * @param newFilePath The full path to the new file that will replace the old one.
     * @return True if the replacement was successful, false otherwise.
     */
    fun overwriteMediaStoreFile(
        context: Context,
        mediaFile: MediaFile,
        newFilePath: String
    ): Boolean {
        val contentResolver = context.contentResolver

        val mediaStoreUri = getMediaStoreUriByID(contentResolver, mediaFile.mediaStoreID)
            ?: return false // File not found in MediaStore
        return try {
            replaceFile(contentResolver, mediaStoreUri, File(newFilePath), mediaFile)
        } catch (e: IOException) {
            DLog.e("replaceMediaStoreFile", "Error replacing file: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Determine if media is marked as favorite in the MediaStore
     *
     * @param context The application context.
     * @param mediaFile Media that you want to check in the MediaStore.
     * @return True if the media is marked as favorite, false otherwise.
     */
    fun mediaIsFavorite(
        context: Context,
        mediaFile: MediaFile
    ): Boolean {
        try {
            val contentResolver = context.contentResolver
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.IS_FAVORITE
            )
            val selection = "${MediaStore.MediaColumns._ID} = ? AND ${MediaStore.MediaColumns.IS_FAVORITE} = 1"
            val selectionArgs = arrayOf(mediaFile.mediaStoreID.toString())

            val queryUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                return (cursor.count != 0)
            }
        }
        catch (e: Exception) {
//            DLog.e(tag, "Error while checking if media is favorite: ${e.message}")
            return false
        }

        return false
    }

    //endregion

    //region Private methods

    /**
     * Gets the MediaStore URI for a given MediaStore ID.
     *
     * @param contentResolver The ContentResolver.
     * @param mediaStoreId The MediaStore ID.
     * @return The MediaStore URI if found, null otherwise.
     */
    private fun getMediaStoreUriByID(contentResolver: ContentResolver, mediaStoreId: Long): Uri? {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns._ID} = ?"
        val selectionArgs = arrayOf(mediaStoreId.toString())

        val queryUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val id = cursor.getLong(idColumn)
                return Uri.withAppendedPath(queryUri, id.toString())
            }
        }

        DLog.e("getMediaStoreUri", "File not found in MediaStore with ID: $mediaStoreId")
        return null
    }


    /**
     * Replaces a file in the MediaStore (supports API 29 (Android 10) and above only)
     *
     * @param contentResolver The ContentResolver.
     * @param mediaStoreUri The MediaStore URI of the file to replace.
     * @param newFile The new file.
     * @return True if successful, false otherwise.
     */
    private fun replaceFile(
        contentResolver: ContentResolver,
        mediaStoreUri: Uri,
        newFile: File,
        mediaFile: MediaFile
    ): Boolean {
        val mediaStoreFile = File(mediaFile.fullPath)
        val savedModificationDate = mediaStoreFile.lastModified()
        contentResolver.openFileDescriptor(mediaStoreUri, "rwt")?.use { mediaFileDescriptor ->
            FileOutputStream(mediaFileDescriptor.fileDescriptor).use { outputStream ->
                FileInputStream(newFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } ?: throw IOException("Failed to open file descriptor for writing")
        mediaStoreFile.setLastModified(savedModificationDate)  //Don't let MediaStore lastModified change
        return true
    }

    //TODO Just for testing/debugging. Not used for app functionality
//    fun cloneAllContentValues(contentResolver: ContentResolver, mediaStoreUri: Uri) : ContentValues? {
//        val columnsToClone = listOf("_id", "duration", "album_artist", "resolution", "orientation", "artist", "author", "format", "height", "is_drm", "volume_name", "date_modified", "writer", "date_expires", "composer", "_display_name", "datetaken", "mime_type", "bitrate", "cd_track_number", "xmp", "year", "_data", "_size", "album", "genre", "title", "width", "is_favorite", "is_trashed", "group_id", "document_id", "generation_added", "is_download", "generation_modified", "is_pending", "date_added", "capture_framerate", "num_tracks", "original_document_id", "bucket_id", "media_type", "relative_path",)
//        val values = ContentValues()
//
//        contentResolver.query(
//            mediaStoreUri,
//            columnsToClone.toTypedArray(),
//            null,
//            null,
//            null
//        )?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                for (columnName in columnsToClone) {
//                    val columnIndex = cursor.getColumnIndex(columnName)
//                    if (columnIndex != -1) {
//                        when (cursor.getType(columnIndex)) {
//                            android.database.Cursor.FIELD_TYPE_NULL -> values.putNull(columnName)
//                            android.database.Cursor.FIELD_TYPE_INTEGER -> values.put(columnName, cursor.getLong(columnIndex))
//                            android.database.Cursor.FIELD_TYPE_FLOAT -> values.put(columnName, cursor.getFloat(columnIndex))
//                            android.database.Cursor.FIELD_TYPE_STRING -> values.put(columnName, cursor.getString(columnIndex))
//                            android.database.Cursor.FIELD_TYPE_BLOB -> values.put(columnName, cursor.getBlob(columnIndex))
//                        }
//                    }
//                }
//            }
//        }
//
//        return values
//    }

    //endregion


}