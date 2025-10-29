package com.tmf.freespace.domainlayer.backgroundworkers

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tmf.freespace.BaseApplication
import com.tmf.freespace.MediaReader
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.IS_IDLE
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.MAX_DATE_ADDED
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.PRIOR_MEDIA_STORE_VERSION
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.models.MediaType
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.domainlayer.compression.CompressionLevels
import com.tmf.freespace.domainlayer.general.ForegroundWorkerUtils
import com.tmf.freespace.domainlayer.general.Permissions
import java.util.concurrent.TimeUnit


class PeriodicBackgroundProcessingWorker(val appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    private val tag = PeriodicBackgroundProcessingWorker::class.simpleName

    /**
     * Worker: Start of periodic processing of background compression tasks
     *
     * . Add each new media file to database
     * . Update potential compression level for all files
     * . Queue SelectFileToCompress + FileUploadDownloadWorker + CompressionWorker chain to process first pending file
     */
    override suspend fun doWork(): Result {
        Log.d(tag, "Starting periodic background processing")

        //Can't process if permissions have been revoked after setup
        if (!Permissions().allPermissionsAreGranted(appContext)) {
            Log.e(tag, "Permissions not granted")
            return Result.failure()
        }

        //Run worker in foreground service to allow to run for up to 6 hours
        if (!ForegroundWorkerUtils().runWorkerAsForegroundService(this, appContext)) {
            Log.e(tag, "Failed to start periodic background processing as foreground service")
            return Result.failure()
        }

        val mediaFileRepository = MediaFileRepository()

        //The MediaStore ID (GUIDs) can change when the MediaStore is rebuilt after a reboot or other (less common) significant event.
        //If this happened, update the MediaStore ID GUIDs in the database, based on the full path to the media
        updateMediaStoreIDsIfRebuilt(mediaFileRepository)

        val maxDateAddedFound = PropertyBag.getLong(MAX_DATE_ADDED, 0L)
        val newMaxDateAddedFound = updateMediaFilesFromMediaStore(maxDateAddedFound, mediaFileRepository, false)  //Add all new media files to DB
        if (newMaxDateAddedFound > maxDateAddedFound) {
            PropertyBag.setLong(MAX_DATE_ADDED, newMaxDateAddedFound)
        }

        updateDesiredCompressionLevelsInDB(mediaFileRepository)  //Update potential compression level for all files

        PropertyBag.setBoolean(IS_IDLE, false)
        val success = FileOptimizationWorker().compressAllPendingMedia()
        PropertyBag.setBoolean(IS_IDLE, true)

        Log.d(tag, "Finished $tag worker processing")
        return if (success) Result.success() else Result.failure()
    }

    //region Private Methods

    /*
     * If the real MediaStore IDs may have changed, update the MediaStore ID GUIDs in the database, based on the full path to the media
     */
    private suspend fun updateMediaStoreIDsIfRebuilt(mediaFileRepository: MediaFileRepository) {
        val priorMediaStoreVersion = PropertyBag.getString(PRIOR_MEDIA_STORE_VERSION, "")
        val newMediaStoreVersion = MediaStore.getVersion(appContext)

        if (priorMediaStoreVersion != newMediaStoreVersion) {
            val maxDateAddedFound = rebuildMediaStore(mediaFileRepository)
            PropertyBag.setString(PRIOR_MEDIA_STORE_VERSION, newMediaStoreVersion)
            PropertyBag.setLong(MAX_DATE_ADDED, maxDateAddedFound)
        }
    }

    /**
     * Rebuild all MediaStore IDs in the database and delete files that were deleted from the device
     */
    private suspend fun rebuildMediaStore(mediaFileRepository: MediaFileRepository) : Long {
        //Mark all existing media files as not updated from MediaStore yet
        mediaFileRepository.markAllMediaAsNotUpdated()

        //Rebuild all MediaStore IDs in the database
        val oldestDateAddedToSelect = PropertyBag.getLong(MAX_DATE_ADDED, 0L)
        val maxDateAddedFound = updateMediaFilesFromMediaStore(oldestDateAddedToSelect, mediaFileRepository, true)

        //Delete files that were deleted from the device (i.e. they are no longer in the database)
        mediaFileRepository.deleteFilesDeletedFromMediaStore()

        return maxDateAddedFound
    }


    /**
     * Fetches media files from MediaStore (all or new) and upserts them into the local database.
     * It also updates existing entries with the latest MediaStore ID if matched by full path.
     *
     * @param oldestDateAddedToSelect The minimum date_added timestamp (seconds) to select files from MediaStore.
     *                             Use 0L to process all files (e.g., during a full rebuild).
     * @param mediaFileRepository Repository to interact with the media file database.
     * @param resyncingMediaStore Flag: Update is while processing rebuilt MediaStore
     * @return The maximum date_added timestamp found among the processed MediaStore files.
     */
    private suspend fun updateMediaFilesFromMediaStore(oldestDateAddedToSelect: Long, mediaFileRepository: MediaFileRepository, resyncingMediaStore: Boolean) : Long {
        var maxDateAddedFound = oldestDateAddedToSelect // Initialize with the input, in case no new files are found
        val mediaReader = MediaReader(appContext)

        mediaReader.getMediaFilesAddedSinceDate(oldestDateAddedToSelect)
            .collect { mediaStoreMediaFile -> // Use collect to consume the Flow
                // Try to find an existing file by fullPath to update its MediaStore ID if it changed
                val existingMediaFile = mediaFileRepository.getMediaFileByFullPath(mediaStoreMediaFile.fullPath)

                val fileToUpsert: MediaFile
                if (resyncingMediaStore && existingMediaFile != null) {
                    // File exists while resyncing MediaStore, the existing file's MediaStoreID might have changed original
                    fileToUpsert = existingMediaFile.copy(
                        mediaStoreID = mediaStoreMediaFile.mediaStoreID,
                        dateInMediaStore = mediaStoreMediaFile.dateInMediaStore
                    )
                    Log.v(tag, "Updating existing file: ${fileToUpsert.fullPath}")
                } else {
                     // New file, use it as is
                    fileToUpsert = mediaStoreMediaFile.copy()
                    Log.v(tag, "Adding new file: ${fileToUpsert.fullPath}")
                }

                mediaFileRepository.upsertMediaFile(fileToUpsert) // Assumes upsert logic: inserts if new, updates if existing (based on PK)

                if (fileToUpsert.dateInMediaStore > maxDateAddedFound) {
                    maxDateAddedFound = fileToUpsert.dateInMediaStore
                }
            }
        return maxDateAddedFound
    }

//    /**
//     * Calculate the amount of space to recover, based on user’s stated free space goal and the current free space on the device
//     */
//    private fun calculateBytesToRecover(): Long {
//        //Get current free space on primary disk
//        val currentFreeSpace = StatFs(Environment.getExternalStorageDirectory().absolutePath).availableBytes
//
//        //Get desired free space from user preferences
//        val desiredFreeSpace = currentFreeSpace + 1000_000_000L  //TODO Get from preferences as (desiredFreeSpaceGB * 1GB)
//
//        return desiredFreeSpace - currentFreeSpace
//    }

    /**
     * Set or update desired compression level for all files in database based on their creation date
     */
    private suspend fun updateDesiredCompressionLevelsInDB(mediaFileRepository: MediaFileRepository) {
        for (compressionLevel in CompressionLevels().compressionLevels) {
            mediaFileRepository.setCompressionLevel(compressionLevel.minDays, compressionLevel.maxDays, compressionLevel.imageCompressionRatio, MediaType.IMAGE)
            mediaFileRepository.setCompressionLevel(compressionLevel.minDays, compressionLevel.maxDays, compressionLevel.videoCompressionRatio, MediaType.VIDEO)
//            mediaFileRepository.setCompressionLevel(compressionLevel.minDays, compressionLevel.maxDays, compressionLevel.audioCompressionRatio, MediaType.AUDIO)
        }
    }

    //endregion


    companion object {
        /**
         * Queue this worker to process periodically (even across reboots)
         */
        fun queuePeriodicProcessing() {
            val context = BaseApplication.instance.baseContext

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<PeriodicBackgroundProcessingWorker>(2, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "FreeSpace_PeriodicBackgroundProcessingWorker",
                ExistingPeriodicWorkPolicy.KEEP,  //Only one copy of worker can be running at a time
                request
            )
        }

        /**
         * Queue this worker to process immediately
         */
        fun queueImmediateProcessing() {
            val context = BaseApplication.instance.baseContext

            val request = OneTimeWorkRequestBuilder<PeriodicBackgroundProcessingWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "FreeSpace_PeriodicBackgroundProcessingWorker",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}