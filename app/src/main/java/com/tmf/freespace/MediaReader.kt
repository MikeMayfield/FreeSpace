package com.tmf.freespace

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.tmf.freespace.files.MediaStoreUtil
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType

class MediaReader(
    private val context: Context
) {
    fun forNewMediaFiles(onNextMediaFile: (MediaFile) -> Unit) {
        // Not having permission on < 33 makes the app crash when attempting to query
        val skipQuery = if (Build.VERSION.SDK_INT <= 32) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        } else false

        if (skipQuery) {
            return
        }

        val queryUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )

        //TODO On API >=30, we can check for just newly added/changed files
        context.contentResolver.query(
            queryUri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val fullPathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val mediaID = cursor.getLong(idColumn)
                val fullPath = cursor.getString(fullPathColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val size = cursor.getInt(sizeColumn)
                val height = cursor.getInt(heightColumn)
                val width = cursor.getInt(widthColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)

                if (fullPath != null && mimeType != null) {
                    val mediaType = when {
                        mimeType.startsWith("audio/") -> MediaType.AUDIO
                        mimeType.startsWith("video/") -> MediaType.VIDEO
                        mimeType.startsWith("image/") -> MediaType.IMAGE
                        else -> null
                    }

                    if (mediaType != null) {
                        val newMediaFile = MediaFile(
                            id = mediaID,
                            fullPath = fullPath,
                            originalSize = size,
                            width = width,
                            height = height,
                            mediaType = mediaType,
                            creationDtm = dateAdded,
                            modifiedDtm = dateModified,
                            isOnServer = false,
                        )
                        onNextMediaFile(newMediaFile)
                    }

                }
            }
        }
    }
}