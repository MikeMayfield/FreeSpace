package com.tmf.freespace

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.models.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MediaReader(
    private val context: Context,
) {

    /**
     * Emits all media files (images and videos) from MediaStore, optionally filtered by a minimum date added.
     *
     * @param oldestDateAddedToSelect Unix timestamp (seconds) to filter media files. Only files added
     *                             on or after this date will be emitted. Use 0L to get all files.
     * @return A Flow that emits MediaFile objects.
     */
    suspend fun getMediaFilesAddedSinceDate(oldestDateAddedToSelect: Long): Flow<MediaFile> = flow {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA, // For file path - use with caution for Scoped Storage
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )

        // Selection for date filtering
        val selection = if (oldestDateAddedToSelect > 0) "${MediaStore.MediaColumns.DATE_ADDED} >= ?" else null
        val selectionArgs = if (oldestDateAddedToSelect > 0) arrayOf(oldestDateAddedToSelect.toString()) else null

        // Query for Images
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} ASC" // Order by date added
        )?.use { cursor -> // Use 'use' to ensure the cursor is closed
            while (cursor.moveToNext()) {
                val mediaFile = createMediaFile(cursor, MediaType.IMAGE)
                emit(mediaFile)
            }
        }

        // Query for Videos (similar logic to images)
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val mediaFile = createMediaFile(cursor, MediaType.VIDEO)
                emit(mediaFile)
            }
        }

        // Query for Audios (similar logic to images)
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val mediaFile = createMediaFile(cursor, MediaType.AUDIO)
                emit(mediaFile)
            }
        }
    }

    private fun createMediaFile(cursor: Cursor, mediaType: MediaType): MediaFile {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
        val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
        val dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
        val size = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)) // Changed to Int to match MediaFile
        val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH))
        val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT))

        // Construct the content URI using the MediaStore ID
        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
            .appendPath(id.toString())
            .build()

        val mediaFile = MediaFile(
            mediaStoreID = id,
            // Use content URI as fullPath if DATA is not reliable or for Scoped Storage best practices
            // For simplicity here, we'll keep using path, but consider contentUri.toString()
            fullPath = path ?: contentUri.toString(), // Fallback to content URI if path is null
            originalSize = size,
            compressedSize = size, // Initially compressedSize is same as original
            width = width,
            height = height,
            mediaType = mediaType,
            currentCompressionRatio = 0,
            desiredCompressionRatio = 0,
            creationDtm = dateAdded, // Assuming creationDtm maps to dateAdded
            modifiedDtm = dateModified,
            dateInMediaStore = dateAdded,
            mediaHasBeenUpdated = true // Default for new files from MediaStore
        )

        return mediaFile
    }

//    /**
//     * Find all media in MediaStore that are newer than newFileBaseDtm.
//     */
//    fun forAllOrNewMediaFiles(minDateAdded: Long, onNextMediaFile: (MediaFile) -> Unit) {
//        // Not having permission on < 33 makes the app crash when attempting to query
//        val skipQuery = if (Build.VERSION.SDK_INT <= 32) {
//            ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            ) != PackageManager.PERMISSION_GRANTED
//        } else false
//
//        if (skipQuery) {
//            return
//        }
//
//        val queryUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//
//        val projection = arrayOf(
//            MediaStore.Files.FileColumns._ID,
//            MediaStore.Files.FileColumns.DATA,
//            MediaStore.Files.FileColumns.MIME_TYPE,
//            MediaStore.Files.FileColumns.SIZE,
//            MediaStore.Files.FileColumns.HEIGHT,
//            MediaStore.Files.FileColumns.WIDTH,
//            MediaStore.Files.FileColumns.DATE_TAKEN,
//            MediaStore.Files.FileColumns.DATE_MODIFIED,
//            MediaStore.Files.FileColumns.DATE_ADDED,
//        )
//
//        val selection = "${MediaStore.Files.FileColumns.DATE_ADDED} >= ?"
//        val selectionArgs = arrayOf(minDateAdded.toString())
//
//        context.contentResolver.query(
//            queryUri,
//            projection,
//            selection,
//            selectionArgs,
//            null
//        )?.use { cursor ->
//            val mediaStoreIDColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
//            val fullPathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
//            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
//            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
//            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
//            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
//            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
//            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
//            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
//
//            while (cursor.moveToNext()) {
//                val mediaStoreID = cursor.getLong(mediaStoreIDColumn)  //TODO Compute MediaStoreID from ID and full path
//                val fullPath = cursor.getString(fullPathColumn)
//                val mimeType = cursor.getString(mimeTypeColumn)
//                val size = cursor.getInt(sizeColumn)
//                val height = cursor.getInt(heightColumn)
//                val width = cursor.getInt(widthColumn)
//                val dateTaken = cursor.getLong(dateTakenColumn)
//                val dateModified = cursor.getLong(dateModifiedColumn)
//                val dateInMediaStore = cursor.getLong(dateAddedColumn)
//
//                if (fullPath != null && mimeType != null) {
//                    val mediaType = when {
//                        mimeType.startsWith("image/") -> MediaType.IMAGE
//                        mimeType.startsWith("video/") -> MediaType.VIDEO
//                        mimeType.startsWith("audio/") -> MediaType.AUDIO
//                        else -> null
//                    }
//
//                    if (mediaType != null) {
//                        val newMediaFile = MediaFile(
//                            mediaStoreID = mediaStoreID,
//                            fullPath = fullPath,
//                            originalSize = size,
//                            compressedSize = size,
//                            width = width,
//                            height = height,
//                            mediaType = mediaType,
//                            creationDtm = dateTaken,
//                            modifiedDtm = dateModified,
//                            serverID = -1,
//                            dateInMediaStore = dateInMediaStore
//                        )
//                        onNextMediaFile(newMediaFile)
//                    }
//                }
//            }
//        }
//    }
}