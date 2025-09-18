package com.tmf.freespace.domainlayer.backgroundworkers

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tmf.freespace.datalayer.mediastore.MediaStoreUtil
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.domainlayer.compression.Compressor
import java.io.File

class FileOptimizationWorker(val appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    private val TAG = FileOptimizationWorker::class.simpleName
    private val mediaFileRepository = MediaFileRepository(appContext)

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
        Log.d(TAG, "Processing files to optimize storage space")
        val timeToReschedule = System.currentTimeMillis() + (5 * 60_000)  //5 minutes from now)
        val compressionLevelThatCanExceedOptimalByteCount = 3  //TODO: Preferences.getInt("ALWAYS_OPTIMIZE_LEVEL", 3)  //Desired compression level(s) that can exceed optimal byte count
        var maxBytesToRecover = calculateMaxBytesToRecover()  //Max bytes is based on limit for users subscription (including FREE plan)
        var optimalBytesToRecover = calculateOptimalBytesToRecover()  //Optimal bytes is based on space needed to reach system free space goal (see Preferences, typically 10GB), but not limited when processing older files

        var fileToCompress = getFileToCompress()
        //Repeat while not over FREE plan limit and not enough space recovered. Allow as many old, high compression files as available
        while (fileToCompress != null && maxBytesToRecover > 0 && (optimalBytesToRecover > 0 || fileToCompress.desiredCompressionLevel >= compressionLevelThatCanExceedOptimalByteCount)) {
            //If processing too long, start at new worker to continue processing files
            if (System.currentTimeMillis() >= timeToReschedule) {
                Log.d(TAG, "Scheduling new worker for next slice of processing")
                scheduleFileOptimizationWorker()
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
                fileToCompress.desiredCompressionLevel = 0
                updateFileToCompressInDB(fileToCompress)
            }

            //Repeat for each remaining file until space recovery goal has been met or allowed time has expired
            fileToCompress = getFileToCompress()
        }

        Log.d(TAG, "Finished processing files to optimize storage space")
        return Result.success()
    }

    private suspend fun calculateMaxBytesToRecover(): Long {
        val maxGBLimitForPlan = 10  //TODO Preferences.getInt("MAX_GB_LIMIT_FOR_PLAN", 10)
        val maxByteLimitForPlan = maxGBLimitForPlan * 1_000_000_000L
        return maxByteLimitForPlan - getBytesRecovered()
    }

    private suspend fun getBytesRecovered(): Long {
        return mediaFileRepository.getBytesRecovered()
    }

    private fun calculateOptimalBytesToRecover(): Long {
        val minGBFreeGoal = 1  //TODO Preferences.getInt("MIN_GB_FREE_GOAL", 10)
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
        val compressedFilePath = compressedFilePath()
        if (Compressor(applicationContext).compress(fileToCompress, uncompressedFilePath, compressedFilePath)) {
            val compressedFileSize = File(compressedFilePath).length().toInt()
            if (compressedFileSize < priorCompressedSize) {
                if (!updateMediaStoreWithCompressedFile(fileToCompress, compressedFilePath)) {
                    return false
                }
            }
            deleteFile(compressedFilePath)
        } else {
            Log.d(TAG, "Error compressing file ${fileToCompress.fullPath}")
            return false
        }

        //Update DB to show compression processed (or not needed)
        fileToCompress.currentCompressionLevel = fileToCompress.desiredCompressionLevel  //Consider file compressed if compression was optimized out
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
                    fileToCompress.currentCompressionLevel = fileToCompress.desiredCompressionLevel
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

    private fun scheduleFileOptimizationWorker() {
        WorkManager.getInstance(appContext).enqueue(buildWorkRequest())
    }

    private fun compressedFilePath(): String {
        return "${appContext.cacheDir.absolutePath}/freespace/compressed.jpg"
    }

    private fun deleteFile(filePath: String?) {
        if (filePath != null) {
            val uncompressedFile = File(filePath)
            if (uncompressedFile.exists()) uncompressedFile.delete()
        }
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