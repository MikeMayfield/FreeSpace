package com.tmf.freespace.uilayer.backgroundworkers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tmf.freespace.datalayer.repositories.MediaFileRepository


/**
 * WorkManager task to select next file to be uploaded/downloaded and compressed (Task #2)
 *
 *  . SelectFileToCompressWorker:
 *  . . Find next file that needs to be compressed, select by highest compression level (desc), file size (desc)
 *  . . Queue FileUploadDownloadWorker to transfer file to/from file server (pass file ID, amount of space to recover),
 *  . .   followed by CompressionWorker (pass file ID, amount of space to recover)
 */

class SelectFileToCompressWorker(val appContext: Context, val params: WorkerParameters): CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val mediaFileRepository = MediaFileRepository(appContext)
        var fileToCompress = mediaFileRepository.getFileToCompress()
        if (fileToCompress == null) {
            return Result.failure()
        }

        //Continue on to next worker in chain, passing it the file ID of the file to compress
        val resultData = Data.Builder()
            .putLong("FileID", fileToCompress.id)
            .build()
        return Result.success(resultData)
    }

    companion object {

        fun buildWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SelectFileToCompressWorker>()
                .setConstraints(constraints)
                .build()

            return workRequest
        }
    }
}
