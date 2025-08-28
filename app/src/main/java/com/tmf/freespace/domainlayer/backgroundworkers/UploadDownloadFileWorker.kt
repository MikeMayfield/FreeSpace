package com.tmf.freespace.domainlayer.backgroundworkers

import User
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.tmf.freespace.datalayer.datasources.network.ServerIO
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.datalayer.repositories.UserRepository
import java.io.File
import java.io.IOException
import androidx.core.net.toUri
import java.io.FileOutputStream


/**
 * WorkManager task to upload a file before compression (Task #3)
 */
class UploadDownloadFileWorker(val appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {

    /**
     * Worker: Upload/download file to/from file server
     *
     * <param>params.FileID</param> - ID of file to upload/download
     * <result/>params.FileID - ID of file to compress
     * <result>params.UncompressedFilePath - Path to uncompressed file to compress
     *
     *
     * . If file not already uploaded
     * . . Upload file to file server
     * . Else
     * . . Download existing file from file server
     */
    override suspend fun doWork(): Result {
        val mediaFileRepository = MediaFileRepository(applicationContext)
        val serverIO = ServerIO()
        val fileID = inputData.getLong(SelectFileToCompressWorker.PARAM_FILE_ID, 0)
        val user = UserRepository(applicationContext).getUser()

        //Get file info from DB
        val mediaFile = mediaFileRepository.getMediaFileByID(fileID)
        if (mediaFile == null) {
            return Result.failure()
        }

        //Upload or download file to/from cloud
        val uncompressedFilePath = if (!mediaFile.isOnServer) {
            uploadFile(mediaFile, user, mediaFileRepository, serverIO)
        } else {
            downloadFile(mediaFile, user, mediaFileRepository, serverIO)
        }
        if (uncompressedFilePath == null) {
            return Result.failure()  //Unable to access cloud server, abort until next processing slot
        }

        //Continue on to next worker in chain, passing it the file ID of the file to compress and the path to the source file to be compressed
        val resultData = Data.Builder()
            .putLong(PARAM_FILE_ID, fileID)
            .putString(PARAM_UNCOMPRESSED_FILE_PATH, uncompressedFilePath)
            .build()
        return Result.success(resultData)
    }

    private suspend fun uploadFile(mediaFile: MediaFile, user: User, mediaFileRepository: MediaFileRepository, serverIO: ServerIO) : String? {
        var transferredSuccessfully = false
        val uncompressedFilePath = extractMediaStoreFile(mediaFile)
        if (uncompressedFilePath == null) {
            return null
        }

        val ftpCredentials = serverIO.allocateFileInCloud(user.idGuid, mediaFile.id, mediaFile.fullPath, mediaFile.originalSize)
        if (ftpCredentials != null) {
            transferredSuccessfully = mediaFileRepository.uploadMediaToCloud(mediaFile, uncompressedFilePath, ftpCredentials)
            if (!transferredSuccessfully)
                mediaFile.serverID = ftpCredentials.serverID
            mediaFileRepository.updateMediaFile(mediaFile)
        }

        return if (transferredSuccessfully) uncompressedFilePath else null
    }

    private suspend fun downloadFile(mediaFile: MediaFile, user: User, mediaFileRepository: MediaFileRepository, serverIO: ServerIO) : String? {
        var uncompressedFilePath : String? = null
        val ftpCredentials = serverIO.getFtpCredentials(user.idGuid, mediaFile.id)
        if (ftpCredentials != null) {
            uncompressedFilePath = uncompressedFilePath(mediaFile.id)
            if (!mediaFileRepository.downloadMediaFromCloud(mediaFile, uncompressedFilePath, ftpCredentials)) {
                uncompressedFilePath = null
            }
        }

        return uncompressedFilePath
    }

    /**
     * Extracts a file from the MediaStore (given its content URI string) to a temporary file
     * in the app's cache directory. The temporary file's name will be derived from the
     * MediaStore ID.
     *
     * @param mediaFile The MediaFile containing the information about the file to extract
     * @return The absolute path to the created temporary file, or null if an error occurred.
     */
    private fun extractMediaStoreFile(mediaFile: MediaFile): String? {
        val contentUri = try {
            mediaFile.fullPath.toUri()
        } catch (e: Exception) {
            e.printStackTrace() // Log error: Invalid URI string
            return null
        }

        if ("content" != contentUri.scheme) {
            // Handle cases where the path might already be a file path or unsupported scheme
            if (File(mediaFile.fullPath).exists() && "file" == contentUri.scheme) { // If it's a file URI and exists
                return contentUri.path
            }
            // Log error or warning: Unsupported URI scheme or not a direct file path
            return null
        }

        val contentResolver = appContext.contentResolver
        var mediaStoreId = getMediaStoreID(contentResolver, contentUri)

        if (mediaStoreId.isNullOrEmpty() || mediaStoreId == "null") { // Added check for "null" string
            mediaStoreId = "temp_media_${System.currentTimeMillis()}"
        }

        return copyMediaStoreFileToCache(contentResolver, contentUri, mediaStoreId)
    }

    private fun getMediaStoreID(contentResolver: ContentResolver, contentUri: Uri): String? {
        // Try to get the display name and MediaStore ID
        var mediaStoreId: String? = null
        try {
            contentResolver.query(contentUri, null, null, null, null)?.use { cursor ->
                var originalFileName: String? = null
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        originalFileName = cursor.getString(displayNameIndex)
                    }

                    // Extract MediaStore ID from the URI's last path segment
                    mediaStoreId = contentUri.lastPathSegment?.substringAfterLast(':')

                    if (mediaStoreId.isNullOrEmpty()) {
                        // Fallback if ID extraction from URI fails, try from DISPLAY_NAME (less reliable)
                        mediaStoreId = originalFileName?.substringBeforeLast('.') ?: "temp_media"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace() // Log error during query
            // Fallback for ID if query fails
            mediaStoreId = contentUri.lastPathSegment?.substringAfterLast(':') ?: "temp_media_uri_fallback"
        }

        return mediaStoreId
    }

    private fun copyMediaStoreFileToCache(contentResolver: ContentResolver, contentUri: Uri, mediaStoreId: String) : String? {
        val tempFileName = "extracted_id_${mediaStoreId}.tmp"
        val tempFile = File(appContext.cacheDir, tempFileName)

        try {
            // Ensure cache directory exists
            if (!appContext.cacheDir.exists()) {
                appContext.cacheDir.mkdirs()
            }
            // Overwrite if file already exists from a previous failed attempt
            if (tempFile.exists()) {
                tempFile.delete()
            }

            contentResolver.openInputStream(contentUri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return null // openInputStream returned null

            return tempFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace() // Log the error
            if (tempFile.exists()) {
                tempFile.delete() // Clean up partially created file
            }
            return null
        } catch (e: SecurityException) {
            e.printStackTrace() // Log permission issues
            return null
        }

    }

    private fun uncompressedFilePath(id: Long): String {
        val cacheSubDir = File(appContext.cacheDir, "freespace_uncompressed")
        if (!cacheSubDir.exists()) {
            cacheSubDir.mkdirs()
        }
        return "${cacheSubDir.absolutePath}/${id}.uncompressed"
    }

    companion object {
        const val PARAM_FILE_ID = "FileID"
        const val PARAM_UNCOMPRESSED_FILE_PATH = "UncompressedFilePath"

        /**
         * Create request to queue this worker
         */
        fun buildWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
//                .setRequiresDeviceIdle(true)
//                .setRequiresStorageNotLow(true)
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<UploadDownloadFileWorker>()
                .setConstraints(constraints)
                .build()

            return workRequest
        }
    }
}