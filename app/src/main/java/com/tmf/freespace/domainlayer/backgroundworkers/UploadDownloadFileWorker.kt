package com.tmf.freespace.domainlayer.backgroundworkers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.tmf.freespace.datalayer.datasources.network.ServerIO
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.datalayer.repositories.UserRepository


/**
 * WorkManager task to upload a file before compression (Task #3)
 */
class UploadDownloadFileWorker(val appContext: Context, val params: WorkerParameters): CoroutineWorker(appContext, params) {

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
        if (user == null) {
            return Result.failure()
        }

        //Get file info from DB
        val mediaFile = mediaFileRepository.getMediaFileByID(fileID)
        if (mediaFile == null) {
            return Result.failure()
        }

        //Upload or download file to/from cloud
        val uncompressedFilePath: String
        var transferredSuccessfully: Boolean
        if (!mediaFile.isOnServer) {
            //Upload to cloud
            uncompressedFilePath = mediaFile.fullPath
            val ftpCredentials = serverIO.allocateFileInCloud(user.idGuid, mediaFile.id, mediaFile.fullPath, mediaFile.originalSize)
            if (ftpCredentials != null) {
                transferredSuccessfully = mediaFileRepository.uploadMediaToCloud(mediaFile, uncompressedFilePath, ftpCredentials)
                if (!transferredSuccessfully)
                    mediaFile.serverID = ftpCredentials.serverID
                mediaFileRepository.updateMediaFile(mediaFile)
                return Result.success()
            }
            return Result.failure()
        }
        else {
            //Download from cloud
            val ftpCredentials = serverIO.getFtpCredentials(user.idGuid, mediaFile.id)
            if (ftpCredentials == null) {
                return Result.failure()
            }
            uncompressedFilePath = uncompressedFilePath(fileID)
            transferredSuccessfully = mediaFileRepository.downloadMediaFromCloud(mediaFile, uncompressedFilePath, ftpCredentials)
        }

        //Unable to access cloud server, abort until next processing slot
        if (!transferredSuccessfully) {
            return Result.failure()
        }

        //Continue on to next worker in chain, passing it the file ID of the file to compress and the path to the source file to be compressed
        val resultData = Data.Builder()
            .putLong(PARAM_FILE_ID, fileID)
            .putString(PARAM_UNCOMPRESSED_FILE_PATH, uncompressedFilePath)
            .build()
        return Result.success(resultData)
    }

    private fun uncompressedFilePath(id: Long): String {
        return "${appContext.cacheDir.absolutePath}/freespace/${id}.uncompressed"
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