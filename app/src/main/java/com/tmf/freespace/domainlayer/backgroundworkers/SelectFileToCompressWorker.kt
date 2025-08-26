package com.tmf.freespace.domainlayer.backgroundworkers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.tmf.freespace.datalayer.datasources.local.dao.MediaFileDao
import com.tmf.freespace.datalayer.datasources.local.database.AppDatabase


/**
 * WorkManager task to select next file to be uploaded/downloaded and compressed (Task #2)
 *
 *  . SelectFileToCompressWorker:
 *  . . Find next file that needs to be compressed, select by highest compression level (desc), file size (desc)
 *  . . Queue FileUploadDownloadWorker to transfer file to/from file server (pass file ID, amount of space to recover),
 *  . .   followed by CompressionWorker (pass file ID, amount of space to recover)
 */

class SelectFileToCompressWorker(val appContext: Context, val params: WorkerParameters): CoroutineWorker(appContext, params) {
    private lateinit var mediaFileDao: MediaFileDao

    override suspend fun doWork(): Result {
        mediaFileDao = AppDatabase.create(appContext).mediaFileDao

        val fileToCompress = mediaFileDao.getFileToCompress()
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
//TODO                .setRequiresDeviceIdle(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SelectFileToCompressWorker>()
                .setConstraints(constraints)
                .build()

            return workRequest
        }
    }
}
