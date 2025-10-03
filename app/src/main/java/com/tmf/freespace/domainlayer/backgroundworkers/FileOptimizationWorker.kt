package com.tmf.freespace.domainlayer.backgroundworkers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tmf.freespace.R
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.mediastore.MediaStoreUtil
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.domainlayer.compression.Compressor
import java.io.File


class FileOptimizationWorker(val appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    private val tag = FileOptimizationWorker::class.simpleName
    private val propertyBag = PropertyBag(appContext)
    private val mediaFileRepository = MediaFileRepository(appContext)

    // Notification constants
    private val NOTIFICATION_ID = 1001 // Unique ID for the notification
    private val NOTIFICATION_CHANNEL_ID = "FileOptimizationChannel" // Unique channel ID

    /**
     * Worker: Process files to optimize storage space for up to about 5 minutes. If not complete in that amount of time,
     * schedule another copy of this worker to be run again until necessary compression has been achieved. Breaking the work
     * into chunks like this allows background processing without using a foreground service.
     *
     * . Determine amount of space to attempt to recover
     * . Repeat for each file file that needs to be compressed, select by highest compression level (desc), file size (desc)
     * . . Compress the uncompressed (original or recovered) media file
     * . . Replace file in MediaStore with compressed file
     */
    override suspend fun doWork(): Result {
        Log.d(tag, "Processing files to optimize storage space as a long running process")

        // Create and display a notification for the foreground service
//        val foregroundInfo = createForegroundInfo("Optimizing files...")
//        try {
//            setForeground(foregroundInfo) // Use suspend version for CoroutineWorker
//        } catch (e: IllegalStateException) {
//            Log.e(tag, "Error setting foreground service. Does the app have FOREGROUND_SERVICE permission? Or is it running on an older API without appropriate service type?", e)
//            return Result.failure() // Or handle as appropriate
//        }

        val timeToReschedule = System.currentTimeMillis() + (1 * 60_000)  //8 minutes from now)  //TODO
        val compressionRatioThatCanExceedOptimalByteCount = propertyBag.getInt("ALWAYS_OPTIMIZE_LEVEL", 5)  //Desired compression level(s) that can exceed optimal byte count
        var maxBytesToRecover = calculateMaxBytesToRecover()  //Max bytes is based on limit for users subscription (including FREE plan)
        var optimalBytesToRecover = calculateOptimalBytesToRecover()  //Optimal bytes is based on space needed to reach system free space goal (see Preferences, typically 10GB), but not limited when processing older files

        var fileToCompress = getFileToCompress()
        //Repeat while not over FREE plan limit and not enough space recovered. Allow as many old, high compression files as available  //TODO Use entire schedule time for video or audio files that might take a long time
        while (fileToCompress != null && maxBytesToRecover > 0 && (optimalBytesToRecover > 0 || fileToCompress.desiredCompressionRatio >= compressionRatioThatCanExceedOptimalByteCount)) {
            //TODO
            //If processing too long, start at new worker to continue processing files
            if (System.currentTimeMillis() >= timeToReschedule) {
                Log.d(tag, "Scheduling new worker for next slice of processing")
                queueFileOptimizationWorker()
                return Result.success()  //Exit this work and start next slice of work
            }

            //Compress file and replace existing file in MediaStore
            val fileSizeBeforeCompression = fileToCompress.compressedSize  //Note:  compressedSize = original file size if not compressed yet
            if (compressFile(fileToCompress)) {
                updateFileToCompressInDB(fileToCompress)
                maxBytesToRecover -= (fileSizeBeforeCompression - fileToCompress.compressedSize)
                optimalBytesToRecover -= (fileSizeBeforeCompression - fileToCompress.compressedSize)
            } else {
                //If problem processing file, exclude it from processing until next pass assigning desired compression levels
                fileToCompress.desiredCompressionRatio = 0
                updateFileToCompressInDB(fileToCompress)
            }

            //Repeat for each remaining file until space recovery goal has been met or allowed time has expired
            fileToCompress = getFileToCompress()
        }

        Log.d(tag, "Finished processing files to optimize storage space")
        return Result.success()
    }


    /**
     * Create the ForegroundInfo for this Worker.
     * This involves creating a notification that will be shown to the user.
     */
    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val title = appContext.getString(R.string.file_optimization_notification_title) // Define in strings.xml
        val cancel = appContext.getString(R.string.file_optimization_notification_cancel) // Define in strings.xml
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        createNotificationChannel() // Ensure channel is created

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_notifications) // Replace with your notification icon
            .setOngoing(true) // Makes the notification non-dismissible
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            // Optionally, set a category and priority
             .setCategory(NotificationCompat.CATEGORY_SERVICE)
             .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // For Android 12 (API 31) and above, you might need to specify foregroundServiceType
        // in the notification if you also declare it in the manifest for the service.
        // However, WorkManager often handles this. If you encounter issues on API 31+,
        // you might need to look into this more.
        // The foreground service type for dataSync or similar might be appropriate if explicitly needed.
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING // Or your relevant type
        } else {
            0 // For older versions, this is not strictly needed in the ForegroundInfo
        }
        return ForegroundInfo(NOTIFICATION_ID, notification, foregroundServiceType)
    }

    /**
     * Creates a notification channel for Android Oreo (API 26) and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = appContext.getString(R.string.file_optimization_channel_name) // Define in strings.xml
            val descriptionText = appContext.getString(R.string.file_optimization_channel_description) // Define in strings.xml
            val importance = NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound/vibration unless critical
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun calculateMaxBytesToRecover(): Long {
        val maxGBLimitForPlan = propertyBag.getInt("MAX_GB_LIMIT_FOR_PLAN", 10)
        val maxByteLimitForPlan = maxGBLimitForPlan * 1_000_000_000L
        return maxByteLimitForPlan - getBytesRecovered()
    }

    private suspend fun getBytesRecovered(): Long {
        return mediaFileRepository.getBytesRecovered()
    }

    private suspend fun calculateOptimalBytesToRecover(): Long {
        //If trial, always try to recover full trial amount remaining to emphasize value of product during trial
        val trialFreeBytesToRecover = propertyBag.getLong("TRIAL_GB_FREE", 10L) * 1_000_000_000 - getBytesRecovered()  //TODO Preferences.getInt("TRIAL_GB_FREE", 10) ...
        if (trialFreeBytesToRecover > 0) {
            return trialFreeBytesToRecover
        }

        //Get goal of space to leave free at all times
        val minGBFreeGoal = propertyBag.getInt("MIN_GB_FREE_GOAL", 10)
        val statFs = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesAvailable = statFs.blockSizeLong * statFs.availableBlocksLong
        return minGBFreeGoal * 1_000_000_000L - bytesAvailable
    }

    private suspend fun getFileToCompress() : MediaFile? {
        return mediaFileRepository.getFileToCompress()  //TODO More sophisticated selection criteria needed
    }

    private suspend fun compressFile(fileToCompress: MediaFile): Boolean {
        //Compress the uncompressed (original or recovered) media file
        val priorCompressedSize = fileToCompress.compressedSize  //NOTE: compressedSize is initially the full file size before any compression
        val uncompressedFilePath = fileToCompress.fullPath
        val compressedFilePath = compressedFilePath(File(uncompressedFilePath).extension)
        if (Compressor(applicationContext).compress(fileToCompress, uncompressedFilePath, compressedFilePath, fileToCompress.desiredCompressionRatio)) {
            val compressedFileSize = File(compressedFilePath).length().toInt()
            if (compressedFileSize < priorCompressedSize) {
                if (!updateMediaStoreWithCompressedFile(fileToCompress, compressedFilePath)) {
                    return false
                }
            }
            deleteFile(compressedFilePath)
        } else {
            Log.d(tag, "Error compressing file ${fileToCompress.fullPath}")
            return false
        }

        //Update DB to show compression processed (or not needed)
        fileToCompress.currentCompressionRatio = fileToCompress.desiredCompressionRatio  //Consider file compressed if compression was optimized out
        mediaFileRepository.updateMediaFile(fileToCompress)

        return true
    }

    private fun updateMediaStoreWithCompressedFile(fileToCompress: MediaFile, compressedFilePath: String) : Boolean {
        val priorCompressedSize = fileToCompress.compressedSize  //NOTE: compressedSize is initially the full file size before any compression
        val compressedFile = File(compressedFilePath)
        if (compressedFile.exists()) {
            try {
                if (MediaStoreUtil().overwriteMediaStoreFile(applicationContext, fileToCompress, compressedFilePath)) {
                    fileToCompress.compressedSize = compressedFile.length().toInt()
                    fileToCompress.currentCompressionRatio = fileToCompress.desiredCompressionRatio
                    Log.d("updateMediaStoreWithCompressedFile", "Updated media store for ${fileToCompress.fullPath} from $priorCompressedSize to ${fileToCompress.compressedSize} bytes")
                    return true
                }
            } catch (e: Exception) {
                Log.e("updateMediaStoreWithCompressedFile", "Error updating media store for ${fileToCompress.fullPath}: ${e.message}")
                return false
            }
        }
        return false
    }

    private suspend fun updateFileToCompressInDB(fileToCompress: MediaFile) {
        mediaFileRepository.updateMediaFile(fileToCompress)
    }

//    private fun scheduleFileOptimizationWorker() {
//        WorkManager.getInstance(appContext).enqueue(buildWorkRequest())
//    }

    private fun compressedFilePath(extension: String): String {
        return "${appContext.cacheDir.absolutePath}/freespace/compressed.$extension"
    }

    private fun deleteFile(filePath: String?) {
        if (filePath != null) {
            val uncompressedFile = File(filePath)
            if (uncompressedFile.exists()) uncompressedFile.delete()
        }
    }

    /**
     * Queue queueFileOptimizationWorker task for processing all pending compressions
     */
    private fun queueFileOptimizationWorker() {
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork("FileOptimizationWorker", ExistingWorkPolicy.APPEND, FileOptimizationWorker.buildWorkRequest())  //Queue file optimization worker after this one
    }


    companion object {
        /**
         * Create request to queue this worker
         */
        fun buildWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<FileOptimizationWorker>()
                .setConstraints(constraints)
                .build()

            return workRequest
        }
    }
}