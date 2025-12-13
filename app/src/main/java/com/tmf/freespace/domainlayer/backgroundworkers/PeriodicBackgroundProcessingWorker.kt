package com.tmf.freespace.domainlayer.backgroundworkers

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
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
import com.tmf.freespace.domainlayer.general.DLog
import com.tmf.freespace.domainlayer.general.ForegroundWorkerUtils
import com.tmf.freespace.domainlayer.general.Permissions
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndDecrement
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.coroutines.cancellation.CancellationException


class PeriodicBackgroundProcessingWorker(val appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    private val tag = "PeriodicBackgroundProcessingWorker"
    private var processingStartTimeMs = 0L

    /**
     * Worker: Start of periodic processing of background compression tasks
     *
     * . Add each new media file to database
     * . Update potential compression level for all files
     * . Queue SelectFileToCompress + FileUploadDownloadWorker + CompressionWorker chain to process first pending file
     */
    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun doWork(): Result {
        val versionName = appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        DLog.d(tag, "--Starting periodic background processing for user '${BaseApplication.firebaseUserID}'. v${versionName}, API ${Build.VERSION.SDK_INT}")
        processingStartTimeMs = System.currentTimeMillis()

        try {
            //Can't process if permissions have been revoked after setup
            if (!Permissions().allPermissionsAreGranted(appContext)) {
                DLog.e(tag, "Permissions not granted")
                return stopRunningWithResult(Result.failure())
            }

            //If we are currently running when this worker is queued, restart it ASAP
            if (currentlyRunning) {
                incrementRestartRequests()
                DLog.d(tag, "Requested restart of $tag processing")
                return Result.success()  //Don't start another service if one is already running
            }

            currentlyRunning = true

            //Run worker in foreground service to allow to run for up to 6 hours
            if (!ForegroundWorkerUtils().runWorkerAsForegroundService(this, appContext)) {
                DLog.e(tag, "Failed to start periodic background processing as foreground service")
                return stopRunningWithResult(Result.failure())
            }

            PropertyBag.setBoolean(IS_IDLE, false)

            val mediaFileRepository = MediaFileRepository()

            do {
                decrementRestartRequests()

                // The MediaStore ID (GUIDs) can change when the MediaStore is rebuilt after a reboot or other (less common) significant event.
                //If this happened, update the MediaStore ID GUIDs in the database, based on the full path to the media
                updateMediaStoreIDsIfRebuilt(mediaFileRepository)

                val priorMaxDateAdded = PropertyBag.getLong(MAX_DATE_ADDED)
                val newMaxDateAdded = updateMediaFilesFromMediaStore(priorMaxDateAdded, mediaFileRepository, false)  //Add all new media files to DB
                if (newMaxDateAdded > priorMaxDateAdded) {
                    PropertyBag.setLong(MAX_DATE_ADDED, newMaxDateAdded)
                }

                updateDesiredCompressionLevelsInDB(mediaFileRepository)  //Update potential compression level for all files

                FileOptimizationWorker().compressAllPendingMedia()
                if (restartRequested()) {
                    DLog.d(tag, "Restarting $tag processing")
                }
            } while (restartRequested())  //NOTE: There is a small race condition here, but the worst case is that processing wait until the next periodic processing instead of running immediately

            PropertyBag.setBoolean(IS_IDLE, true)

            DLog.d(tag, "--Finished $tag worker processing after ${System.currentTimeMillis() - processingStartTimeMs} ms")
            return stopRunningWithResult(Result.success())
        }
        catch (e: Exception) {
            PropertyBag.setString(IS_IDLE, "true")
            if (e is CancellationException) {
                DLog.d(tag, "--User cancelled $tag worker after ${System.currentTimeMillis() - processingStartTimeMs} ms")
                return stopRunningWithResult(Result.success())
            } else {
                DLog.e(tag, "--Error in $tag worker after ${System.currentTimeMillis() - processingStartTimeMs} ms: ${e.message}")
                return stopRunningWithResult(Result.failure())
            }
        }
    }


    //region Private Methods

    /**
     * Sets the 'currentlyRunning' flag to false and returns the provided work result.
     * This is a helper method to ensure the running state is always reset before exiting the worker.
     *
     * @param result The [Result] to be returned by the worker (e.g., Result.success(), Result.failure()).
     * @return The same [Result] object that was passed in.
     */
    private fun stopRunningWithResult(result: Result): Result {
        currentlyRunning = false
        return result
    }

    /*
     * If the real MediaStore IDs may have changed, update the MediaStore ID GUIDs in the database, based on the full path to the media
     */
    private suspend fun updateMediaStoreIDsIfRebuilt(mediaFileRepository: MediaFileRepository) {
        val priorMediaStoreVersion = PropertyBag.getString(PRIOR_MEDIA_STORE_VERSION)
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
        val oldestDateAddedToSelect = PropertyBag.getLong(MAX_DATE_ADDED)
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
                if (restartRequested()) {
                    return@collect
                }

                // Try to find an existing file by fullPath to update its MediaStore ID if it changed
                val existingMediaFile = mediaFileRepository.getMediaFileByFullPath(mediaStoreMediaFile.fullPath)

                val fileToUpsert: MediaFile
                if (resyncingMediaStore && existingMediaFile != null) {
                    // File exists while resyncing MediaStore, the existing file's MediaStoreID might have changed original
                    fileToUpsert = existingMediaFile.copy(
                        mediaStoreID = mediaStoreMediaFile.mediaStoreID,
                        dateInMediaStore = mediaStoreMediaFile.dateInMediaStore
                    )
                    DLog.d(tag, "Updating existing file: ${fileToUpsert.fullPath}")
                } else {
                    // New file, use it as is
                    fileToUpsert = mediaStoreMediaFile.copy()
                    DLog.d(tag, "Adding new file: ${fileToUpsert.fullPath}")
                }

                mediaFileRepository.upsertMediaFile(fileToUpsert) // Assumes upsert logic: inserts if new, updates if existing (based on PK)

                if (fileToUpsert.dateInMediaStore > maxDateAddedFound) {
                    maxDateAddedFound = fileToUpsert.dateInMediaStore
                }
            }
        return maxDateAddedFound
    }

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
            val request = PeriodicWorkRequestBuilder<PeriodicBackgroundProcessingWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(BaseApplication.instance.baseContext).enqueueUniquePeriodicWork(
                "FreeSpace_PeriodicBackgroundProcessingWorker",
                ExistingPeriodicWorkPolicy.UPDATE,  //Only one copy of worker can be running at a time
                request
            )
        }

        /**
         * Queue this worker to process immediately
         */
        fun queueImmediateProcessing() {
            val request = OneTimeWorkRequestBuilder<PeriodicBackgroundProcessingWorker>().build()
            WorkManager.getInstance(BaseApplication.instance.baseContext).enqueue(request)
        }


        @Volatile
        var currentlyRunning = false  //Flag: The service is currently running
        @Volatile @OptIn(ExperimentalAtomicApi::class)
        private var restartRequestCount = AtomicInt(0)

        @OptIn(ExperimentalAtomicApi::class)
        fun restartRequested(): Boolean {
            return restartRequestCount.load() > 0
        }

        @OptIn(ExperimentalAtomicApi::class)
        private fun incrementRestartRequests() {
            restartRequestCount.fetchAndIncrement()
        }

        @OptIn(ExperimentalAtomicApi::class)
        private fun decrementRestartRequests() {
            val wasZero = restartRequestCount.fetchAndDecrement() == 0
            if (wasZero) {
                incrementRestartRequests()
            }
        }
    }
}