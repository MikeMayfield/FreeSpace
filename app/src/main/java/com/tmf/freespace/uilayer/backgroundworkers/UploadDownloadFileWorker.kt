package com.tmf.freespace.uilayer.backgroundworkers

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.tmf.freespace.datalayer.datasources.cloudstorage.ICloudStorage
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.models.MediaFile

/**
 * WorkManager task to upload a file before compression (Task #3)
 */
class UploadDownloadFileWorker(val appContext: Context, val params: WorkerParameters): CoroutineWorker(appContext, params) {
    //TODO Add support for foreground service if working takes longer than almost 10 minutes to complete
    var mediaFileRepository = MediaFileRepository(appContext)

    /**
     * Upload/download file to/from file server
     *
     * <param>params.FileID</param> - ID of file to upload/download
     * <result/>params.FileID, params.SourceFilePath
     */
    override suspend fun doWork(): Result {
        val fileID = inputData.getLong("FileID", 0)

        //Get file info from DB
        val mediaFile = mediaFileRepository.getMediaFileByID(fileID)
        if (mediaFile == null) {
            return Result.failure()
        }

        //Upload or download file to/from cloud
        var uncompressedFileUri: Uri?
        if (!mediaFile.isOnServer) {
            uncompressedFileUri = mediaFileRepository.uploadFileToCloud(mediaFile)
        }
        else {
            uncompressedFileUri = mediaFileRepository.downloadFileFromCloud(mediaFile)
        }

        //Unable to access cloud server, abort until next processing slot
        if (uncompressedFileUri == null) {
            return Result.failure()
        }

        //Continue on to next worker in chain, passing it the file ID of the file to compress and the path to the source file to be compressed
        val resultData = Data.Builder()
            .putLong("FileID", fileID)
            .putString("UncompressedFileUri", uncompressedFileUri.toString())
            .build()
        return Result.success(resultData)
    }

    companion object {
        /**
         * Create request to queue this worker
         */
        fun buildWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .setRequiresStorageNotLow(true)
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