package com.tmf.freespace.domainlayer.backgroundworkers

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tmf.freespace.MediaReader
import com.tmf.freespace.datalayer.models.MediaType
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.datalayer.repositories.UserRepository
import com.tmf.freespace.domainlayer.compression.CompressionLevels


/**
 * Start of periodic processing of background compression tasks (Task #1)
 *
 * Work flow:
 *  PeriodicBackgroundProcessingWorker -> SelectFileToCompressWorker -> FileUploadDownloadWorker -> CompressionWorker |
 *                                        ^-----------------------------------------------------------------------------
 *
 *  PeriodicBackgroundProcessingWorker:
 *  . Add each new media file to database
 *  . Update potential compression level for all files
 *  . Queue SelectFileToCompress + FileUploadDownloadWorker + CompressionWorker chain to process first pending file
 *
 *  SelectFileToCompressWorker:
 *  . Determine amount of space to recover
 *  . If no space needs to be recovered
 *  . . Abort WorkManager chain
 *  . Find next file that needs to be compressed, select by highest compression level (desc), file size (desc)
 *  . If no file found
 *  . . Abort WorkManager chain
 *
 *  FileUploadDownloadWorker:
 *  . If file not already uploaded
 *  . . Upload file to file server
 *  . . Update file upload status in DB
 *  . Else
 *  . . Download file from file server
 *  . If file transfer was unsuccessful
 *  . . Abort WorkManager chain
 *
 *  CompressionWorker:
 *  . Compress file
 *  . Update file in MediaStore
 *  . Delete temp full-quality file and (?) compressed file
 *  . Queue SelectFileToCompress + FileUploadDownloadWorker + CompressionWorker chain
 */

class PeriodicBackgroundProcessingWorker(val appContext: Context, val params: WorkerParameters): CoroutineWorker(appContext, params) {
    /**
     * Worker: Start of periodic processing of background compression tasks
     *
     * . Add each new media file to database
     * . Update potential compression level for all files
     * . Queue SelectFileToCompress + FileUploadDownloadWorker + CompressionWorker chain to process first pending file
     */
    override suspend fun doWork(): Result {
        val mediaFileRepository = MediaFileRepository(appContext)

        //Send heartbeat to server
        UserRepository(appContext).sendHeartbeat()  //TODO()

        val bytesToRecover = calculateBytesToRecover()  //Determine amount of disk space to recover, based on user’s stated free space goal
        if (bytesToRecover > 0) {
            addAllNewMediaFilesToDB(mediaFileRepository)  //Add all new media files to DB
            updateDesiredCompressionLevelsInDB(mediaFileRepository)  //Update potential compression level for all files

            queueWorkerToProcessFirstFile()  //Queue SelectFileToCompress worker chain for first file to be processed (will requeue itself for each additional file needed)
        }

        return Result.success()
    }

    //region Private Methods

    /**
     * Calculate the amount of space to recover, based on user’s stated free space goal and the current free space on the device
     */
    private fun calculateBytesToRecover(): Long {
        //Get current free space on primary disk
        val currentFreeSpace = StatFs(Environment.getExternalStorageDirectory().absolutePath).availableBytes

        //Get desired free space from user preferences
        val desiredFreeSpace = currentFreeSpace + 1000_000_000L  //TODO Get from preferences as (desiredFreeSpaceGB * 1GB)

        return desiredFreeSpace - currentFreeSpace
    }

    /**
     * Find all new media files on disk and add them to the database for possible future processing
     */
    private fun addAllNewMediaFilesToDB(mediaFileRepository: MediaFileRepository) {
        MediaReader(appContext).forNewMediaFiles { mediaFile ->
            mediaFileRepository.addMediaFile(mediaFile)
        }
    }

    /**
     * Set or update desired compression level for all files in database based on their creation date
     */
    private suspend fun updateDesiredCompressionLevelsInDB(mediaFileRepository: MediaFileRepository) {
        val nowSecs = System.currentTimeMillis() / 1_000L
        val secondsPerDay: Long = 60 * 60 * 24
        for (compressionLevel in CompressionLevels().compressionLevels) {
            mediaFileRepository.setCompressionLevel(
                minAgeDays = nowSecs - compressionLevel.minDays * secondsPerDay,
                maxAgeDays = nowSecs - compressionLevel.maxDays * secondsPerDay,
                compressionLevel = compressionLevel.imageCompressionLevel,
                mediaType = MediaType.IMAGE.ordinal)
            mediaFileRepository.setCompressionLevel(nowSecs - compressionLevel.minDays * secondsPerDay, nowSecs - compressionLevel.maxDays * secondsPerDay,
                compressionLevel.videoCompressionLevel, MediaType.VIDEO.ordinal)
        }
    }

    /**
     * Queue SelectFileToCompress + UploadDownloadFileWorker + CompressionWorker worker chain for first file to be processed.
     * The chain will requeue itself for each additional file needed.
     * NOTE: Chain is not added if chain is already queued/running, in case chain is being requeued from next scheduled compression processing
     */
    private fun queueWorkerToProcessFirstFile() {
        WorkManager.getInstance(appContext)
            .beginUniqueWork(  //Task 1: Select next file to compress
                SelectFileToCompressWorker::class.java.simpleName + "_chain",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SelectFileToCompressWorker>().build()
            )
            .then(UploadDownloadFileWorker.buildWorkRequest())  //Task 2: Upload/download file to/from file server
            .then(CompressionWorker.buildWorkRequest())  //Task 3: Compress file and update MediaStore
            .then(SelectFileToCompressWorker.buildWorkRequest())  //Task 4: Start over on next file to compress (exits when finished compressing)
            .enqueue()
    }

    //endregion


    companion object {
        /**
         * Queue this worker to process periodically (even across reboots)
         */
        fun queuePeriodicProcessing(context: Context, periodHours: Long) {
            //TODO Code below allows immediate execution of PeriodicBackgroundProcessingWorker for testing. REMOVE IT
            val request = OneTimeWorkRequestBuilder<PeriodicBackgroundProcessingWorker>()
                // You can add input data here if needed using .setInputData(workDataOf(...))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                PeriodicBackgroundProcessingWorker::class.java.simpleName,
                ExistingWorkPolicy.REPLACE,  //Don't queue if already running or queued  //TODO Change to KEEP
                request
            )
//TODO Replace above with this            val constraints = Constraints.Builder()
//                .setRequiresDeviceIdle(true)
//                .setRequiresBatteryNotLow(true)
//                .build()
//
//            val request = PeriodicWorkRequestBuilder<ScheduledBackgroundCompressionWorker>(periodHours, TimeUnit.HOURS)
//                .setConstraints(constraints)
//                .build()
//
//            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//                ScheduledBackgroundCompressionWorker::class.java.simpleName,
//                ExistingPeriodicWorkPolicy.KEEP,  //Only one copy of worker can be running at a time
//                request
//            )
        }
    }
}