package com.tmf.freespace.domainlayer.backgroundworkers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import java.io.File


/**
 * WorkManager task to select next file to be uploaded/downloaded and compressed (Task #2)
 *
 *  . SelectFileToCompressWorker:
 *  . . Find next file that needs to be compressed, select by highest compression level (desc), file size (desc)
 *  . . Queue FileUploadDownloadWorker to transfer file to/from file server (pass file ID, amount of space to recover),
 *  . .   followed by CompressionWorker (pass file ID, amount of space to recover)
 */

class SelectFileToCompressWorker(val appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    private lateinit var mediaFileRepository: MediaFileRepository

    /**
     * Worker: Select next file to be uploaded/downloaded and compressed
     *
     * <result/>params.FileID
     *
     * . Find next file that needs to be compressed, select by highest compression level (desc), file size (desc)
     * . Pass file to next worker in chain
     */
    override suspend fun doWork(): Result {
        var fileHasBeenDeleted = true
        var fileToCompress: MediaFile? = null

        mediaFileRepository = MediaFileRepository(appContext)

        while (fileHasBeenDeleted) {
            fileToCompress = mediaFileRepository.getFileToCompress()
            if (fileToCompress == null) {
                Log.d("SelectFileToCompressWorker.doWork", "No more files to compress")
                return Result.failure()  //No more files to compress, abort chain until next scheduled processing time
            }

            /**
             * If media file has been deleted, remove it from the database
             */
            fileHasBeenDeleted = hasFileBeenDeleted(fileToCompress)
            if (fileHasBeenDeleted) {
                Log.d("SelectFileToCompressWorker.doWork", "File ${fileToCompress.id} has been deleted, removing from database")
                mediaFileRepository.deleteFile(fileToCompress)
            }
        }

//        WorkManager.getInstance(appContext).cancelAllWork()  //TODO Remove

        //Start work chain to process this file and then come back to this worker to select next file to process, if any
        //  NOTE: Chain is not added if chain is already queued/running, in case chain is being requeued from next scheduled compression processing
        WorkManager.getInstance(appContext)
            .beginUniqueWork(  //Task 1: Download/upload file to/from file server
                UploadDownloadFileWorker::class.java.simpleName + "_chain",
                ExistingWorkPolicy.KEEP,
                UploadDownloadFileWorker.buildWorkRequest(fileToCompress!!.id)
            )
            .then(CompressionWorker.buildWorkRequest())  //Task 2: Compress file and update MediaStore
            .then(buildWorkRequest())  //Task 3: Start over, selecting next file to compress (exits when finished all pending files)
            .enqueue()

        //Continue on to first worker in new chain (UploadDownloadFileWorker), passing it the file ID of the file to compress
        return Result.success()
    }

    private fun hasFileBeenDeleted(fileToCompress: MediaFile): Boolean {
        return !File(fileToCompress.fullPath).exists()
    }

    companion object {
        const val PARAM_FILE_ID = "FileID"

        fun buildWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
//TODO                .setRequiresDeviceIdle(true)  //TODO Is this needed?
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SelectFileToCompressWorker>()
                .setConstraints(constraints)
                .build()

            return workRequest
        }
    }
}
