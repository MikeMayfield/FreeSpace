package com.tmf.freespace.files

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.tmf.freespace.models.MediaFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MediaStoreUtil {
    //region Public methods

    //Extract media from MediaStore to a temp file for independent processing
    fun extractFileFromMediaStore(context: Context, mediaFile: MediaFile) : String? {
        return extractFileFromMediaStore(context, mediaFile, "${context.cacheDir}/freespace/")
    }

    //Replace the content of media in MediaStore with an updated (compressed) version of the original file
    fun replaceFileInMediaStore(context: Context, mediaFile: MediaFile, newFilePath: String) : Boolean {
        return replaceMediaStoreFile(context, mediaFile, newFilePath)
    }

    //endregion

    //region Private methods

    /**
     * Extracts a file from the MediaStore given its MediaStore ID.
     *
     * @param context The application context.
     * @param mediaFile Media that you want to replace in the MediaStore.
     * @param outputDirectoryPath The directory where the extracted file should be saved.
     * @return The File object representing the extracted file, or null if the extraction failed.
     */
    private fun extractFileFromMediaStore(
        context: Context,
        mediaFile: MediaFile,
        outputDirectoryPath: String
    ): String? {
        val contentResolver = context.contentResolver

        // 1. Get the MediaStore URI for the file
        val mediaStoreUri = getMediaStoreUri(contentResolver, mediaFile.id)
            ?: return null // File not found in MediaStore

        // 2. Create the output file
        val outputDirectory = File(outputDirectoryPath)
        val outputFile = File(outputDirectory, "mediaFile.${mediaFile.fileType}")
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            Log.e("extractFileFromMediaStore", "Failed to create output directory: ${outputDirectory.absolutePath}")
            return null
        }

        // 3. Extract the file
        return try {
            contentResolver.openInputStream(mediaStoreUri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            outputFile.absolutePath

        } catch (e: IOException) {
            Log.e("extractFileFromMediaStore", "Error extracting file: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Replaces a file in the MediaStore with a different file.
     *
     * @param context The application context.
     * @param mediaFile Media that you want to replace in the MediaStore.
     * @param newFilePath The full path to the new file that will replace the old one.
     * @return True if the replacement was successful, false otherwise.
     */
    private fun replaceMediaStoreFile(
        context: Context,
        mediaFile: MediaFile,
        newFilePath: String
    ): Boolean {
        val contentResolver = context.contentResolver

        val mediaStoreUri = getMediaStoreUri(contentResolver, mediaFile.id)
            ?: return false // File not found in MediaStore
        return try {
            replaceFile(contentResolver, mediaStoreUri, File(newFilePath))
        } catch (e: IOException) {
            Log.e("replaceMediaStoreFile", "Error replacing file: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Gets the MediaStore URI for a given MediaStore ID.
     *
     * @param contentResolver The ContentResolver.
     * @param mediaStoreId The MediaStore ID.
     * @return The MediaStore URI if found, null otherwise.
     */
    private fun getMediaStoreUri(contentResolver: ContentResolver, mediaStoreId: Long): Uri? {
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

        Log.e("getMediaStoreUri", "File not found in MediaStore with ID: $mediaStoreId")
        return null
    }

    /**
     * Gets the display name of a file from its MediaStore URI.
     *
     * @param contentResolver The ContentResolver.
     * @param mediaStoreUri The MediaStore URI.
     * @return The display name if found, null otherwise.
     */
    private fun getFileDisplayName(contentResolver: ContentResolver, mediaStoreUri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        contentResolver.query(
            mediaStoreUri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                return cursor.getString(displayNameColumn)
            }
        }
        Log.e("getFileDisplayName", "Display name not found for URI: $mediaStoreUri")
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
        newFile: File
    ): Boolean {
        contentResolver.openFileDescriptor(mediaStoreUri, "rwt")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { outputStream ->
                FileInputStream(newFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } ?:
            throw IOException("Failed to open file descriptor for writing")
        return true
    }

    //TODO Just for testing/debugging. Not used for app functionality
    fun cloneAllContentValues(contentResolver: ContentResolver, mediaStoreUri: Uri) : ContentValues? {
        val columnsToClone = listOf(MediaStore.Audio.Playlists.OWNER_PACKAGE_NAME, "_id", "duration", "album_artist", "resolution", "orientation", "artist", "author", "format", "height", "is_drm", "volume_name", "date_modified", "writer", "date_expires", "composer", "_display_name", "datetaken", "mime_type", "bitrate", "cd_track_number", "xmp", "year", "_data", "_size", "album", "genre", "title", "width", "is_favorite", "is_trashed", "group_id", "document_id", "generation_added", "is_download", "generation_modified", "is_pending", "date_added", "capture_framerate", "num_tracks", "original_document_id", "bucket_id", "media_type", "relative_path",)
        val values = ContentValues()

        contentResolver.query(
            mediaStoreUri,
            columnsToClone.toTypedArray(),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                for (columnName in columnsToClone) {
                    val columnIndex = cursor.getColumnIndex(columnName)
                    if (columnIndex != -1) {
                        when (cursor.getType(columnIndex)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> values.putNull(columnName)
                            android.database.Cursor.FIELD_TYPE_INTEGER -> values.put(columnName, cursor.getLong(columnIndex))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> values.put(columnName, cursor.getFloat(columnIndex))
                            android.database.Cursor.FIELD_TYPE_STRING -> values.put(columnName, cursor.getString(columnIndex))
                            android.database.Cursor.FIELD_TYPE_BLOB -> values.put(columnName, cursor.getBlob(columnIndex))
                        }
                    }
                }
            }
        }

        return values
    }

    //endregion
}