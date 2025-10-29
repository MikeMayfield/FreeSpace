package com.tmf.freespace.domainlayer.backgroundworkers

import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.tmf.freespace.BaseApplication
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.ALWAYS_OPTIMIZE_LEVEL
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.MAX_GB_LIMIT_FOR_PLAN
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.MIN_FREE_SPACE_GOAL_MB
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.TRIAL_GB_FREE
import com.tmf.freespace.datalayer.mediastore.MediaStoreUtil
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.domainlayer.compression.Compressor
import java.io.File
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class FileOptimizationWorker() {
//    class FileOptimizationWorker(val appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    private val tag = FileOptimizationWorker::class.simpleName

    private val appContext = BaseApplication.instance.applicationContext
    private val mediaFileRepository = MediaFileRepository()

    /**
     * Worker: Process files to optimize storage space for all media with pending compression requests, up to limits
     *
     * . Determine amount of space to attempt to recover
     * . Repeat for each file file that needs to be compressed, select by highest compression level (desc), file size (desc)
     * . . Compress the uncompressed (original or recovered) media file
     * . . Replace file in MediaStore with compressed file
     */
    suspend fun compressAllPendingMedia(): Boolean {
        Log.d(tag, "Processing files to optimize storage space")

        val compressionRatioThatCanExceedOptimalByteCount = PropertyBag.getInt(ALWAYS_OPTIMIZE_LEVEL, 5)  //Desired compression level(s) that can exceed optimal byte count
        var maxBytesToRecover = calculateMaxBytesToRecover()  //Max bytes is based on limit for users subscription (including FREE plan)
        var optimalBytesToRecover = calculateOptimalBytesToRecover()  //Optimal bytes is based on space needed to reach system free space goal (see Preferences, typically 5GB), but not limited when processing older files
        val mediaStoreUtil = MediaStoreUtil()

        //Repeat while not over FREE plan limit and not enough space recovered. Allow as many old, high compression files as available  //TODO Use entire schedule time for video or audio files that might take a long time
        var fileToCompress = getFileToCompress()
        while (fileToCompress != null
                && maxBytesToRecover > 0
                && (optimalBytesToRecover > 0 || fileToCompress.desiredCompressionRatio >= compressionRatioThatCanExceedOptimalByteCount)
                && !batteryLow()
                && !hasRunTooLong() ) {
            //Compress file and replace existing file in MediaStore
            if (!mediaStoreUtil.mediaIsFavorite(appContext, fileToCompress)) {  //Don't compress media marked as favorite
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
            } else {
                fileToCompress.currentCompressionRatio = fileToCompress.desiredCompressionRatio  //Consider file compressed file shouldn't be compressed so that it isn't processed again
                updateFileToCompressInDB(fileToCompress)
            }

            //Repeat for each remaining file until space recovery goal has been met or allowed time has expired
            fileToCompress = getFileToCompress()
        }

        Log.d(tag, "Finished processing files to optimize storage space")
        return true
    }

    private suspend fun calculateMaxBytesToRecover(): Long {
        val maxGBLimitForPlan = PropertyBag.getInt(MAX_GB_LIMIT_FOR_PLAN, 8)
        val maxByteLimitForPlan = maxGBLimitForPlan * 1_000_000_000L
        return maxByteLimitForPlan - getBytesRecovered()
    }

    private suspend fun getBytesRecovered(): Long {
        return mediaFileRepository.getBytesRecovered()
    }

    private suspend fun calculateOptimalBytesToRecover(): Long {
        //If trial, always try to recover full trial amount remaining to emphasize value of product during trial
        if (PropertyBag.getString(PropertyBag.SUBSCRIPTION_STATUS, "") == "NOT_SUBSCRIBED") {
            val trialFreeBytesToRecover = PropertyBag.getLong(TRIAL_GB_FREE, 8L) * 1_000_000_000 - getBytesRecovered()
            if (trialFreeBytesToRecover > 0) {
                return trialFreeBytesToRecover
            }
        }

        //Get goal of space to leave free at all times
        val statFs = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesAvailable = statFs.blockSizeLong * statFs.availableBlocksLong
        val minFreeSpaceGoalMB = PropertyBag.getInt(MIN_FREE_SPACE_GOAL_MB, 5_000)
        return minFreeSpaceGoalMB * 1_000_000L - bytesAvailable
    }

    private suspend fun getFileToCompress() : MediaFile? {
        return mediaFileRepository.getFileToCompress()  //TODO More sophisticated selection criteria needed
    }

    private suspend fun compressFile(fileToCompress: MediaFile): Boolean {
        //Compress the uncompressed (original or recovered) media file
        val priorCompressedSize = fileToCompress.compressedSize  //NOTE: compressedSize is initially the full file size before any compression
        val uncompressedFilePath = fileToCompress.fullPath
        val compressedFilePath = compressedFilePath(File(uncompressedFilePath).extension)
        if (Compressor(appContext).compress(fileToCompress, uncompressedFilePath, compressedFilePath, fileToCompress.desiredCompressionRatio)) {
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
                if (MediaStoreUtil().overwriteMediaStoreFile(appContext, fileToCompress, compressedFilePath)) {
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

    private val batteryLowLevel = 33  //Battery level considered to low to process in background
    private fun batteryLow(): Boolean {
        val batteryManager = appContext.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batterLevelPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        return if (batterLevelPct <= batteryLowLevel) {
            Log.d(tag, "Battery low: $batterLevelPct%")
            true
        } else {
            false
        }
    }

    @OptIn(ExperimentalTime::class)
    private val runStarted = Clock.System.now()
    @OptIn(ExperimentalTime::class)
    private fun hasRunTooLong(): Boolean {
        val runDuration = Clock.System.now() - runStarted
        return (runDuration.inWholeMinutes > 5 * 60 + 30)  //Run for up to 5 hours and 30 minutes. This give a buffer of 30 in case we have to compress a large video that takes a long time
    }
}