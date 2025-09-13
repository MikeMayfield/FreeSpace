package com.tmf.freespace.domainlayer.backgroundworkers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.tmf.freespace.datalayer.mediastore.MediaStoreUtil
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.domainlayer.compression.Compressor
import java.io.File
import java.util.UUID

/**
 * WorkManager task to compress (or re-compress) a file (Task #4)
 *
 * @param FileID ID of file to compress
 * @param UncompressedFileUri URI (as string) to uncompressed file to compress
 */
@Suppress("KDocUnresolvedReference")
class CompressionWorker(val appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    //TODO Add support for foreground service if working takes longer than almost 10 minutes to complete

    /**
     * Worker: Compress (or re-compress) a file
     *
     * <param>params.FileID</param> - ID of file to compress
     * <param>params.UncompressedFilePath</param> - Path to uncompressed file to compress
     *
     * . Compress the uncompressed (original or recovered) media file
     * . Replace file in MediaStore with compressed file
     * . Delete temporary files (if any)
     */
    override suspend fun doWork(): Result {
        val emptyUUI = UUID(0L, 0L)
        val fileID = UUID(inputData.getLong(UploadDownloadFileWorker.PARAM_FILE_ID_MSB, 0L), inputData.getLong(UploadDownloadFileWorker.PARAM_FILE_ID_LSB, 0L))
            if (fileID == emptyUUI) {
                Log.w("compressSelectedFiles.doWork", "No file ID provided. File not being compressed")
                return Result.success()  //If no file was processed, nothing left to do but continue to possible next file
            }

            val uncompressedFilePath = inputData.getString(UploadDownloadFileWorker.PARAM_UNCOMPRESSED_FILE_PATH)
        val mediaFileRepository = MediaFileRepository(applicationContext)


        val mediaFile = mediaFileRepository.getMediaFileByID(fileID)
        if (mediaFile != null) {
            //Compress the uncompressed (original or recovered) media file
            val priorCompressedSize = mediaFile.compressedSize  //NOTE: compressedSize is initially the full file size before any compression
            val compressedFilePath = compressedFilePath(fileID)
            if (Compressor(applicationContext).compress(mediaFile, uncompressedFilePath!!, compressedFilePath)) {
                val compressedFileSize = File(compressedFilePath).length().toInt()
                if (compressedFileSize < priorCompressedSize) {
                    deleteFile(uncompressedFilePath)  //Delete temporary uncompressed file to provide more space for creating file in MediaStore
                    updateMediaStoreWithCompressedFile(mediaFile, compressedFilePath)  //Update file in MediaStore with compressed file
                    mediaFile.compressedSize = compressedFileSize
                }
                deleteFile(compressedFilePath)
            }

            //Update DB to show compression processed (or not needed)
            mediaFile.currentCompressionLevel = mediaFile.desiredCompressionLevel  //Consider file compressed if compression was optimized out
            mediaFileRepository.updateMediaFile(mediaFile)

            //Delete temporary uncompressed file (if any)
            deleteFile(uncompressedFilePath)
        } else {
            Log.e("compressSelectedFiles.doWork", "FileID $fileID not found in database")
        }

        return Result.success()
    }

    private fun deleteFile(filePath: String?) {
        if (filePath != null) {
            val uncompressedFile = File(filePath)
            if (uncompressedFile.exists()) uncompressedFile.delete()
        }
    }

    private fun compressedFilePath(mediaFileID: UUID): String {
        return "${appContext.cacheDir.absolutePath}/freespace/${mediaFileID}.compressed"
    }

    private fun updateMediaStoreWithCompressedFile(mediaFile: MediaFile, compressedFilePath: String) : Boolean {
        val priorCompressedSize = mediaFile.compressedSize  //NOTE: compressedSize is initially the full file size before any compression
        val compressedFile = File(compressedFilePath)
        if (compressedFile.exists()) {
            try {
                if (MediaStoreUtil().overwriteMediaStoreFile(applicationContext, mediaFile, compressedFilePath)) {
                    mediaFile.compressedSize = compressedFile.length().toInt()
                    mediaFile.currentCompressionLevel = mediaFile.desiredCompressionLevel
                    Log.d("updateMediaStoreWithCompressedFile", "Updated media store for ${mediaFile.fullPath} from $priorCompressedSize to ${mediaFile.compressedSize} bytes")
                    return true
                }
            } catch (e: Exception) {
                Log.e("updateMediaStoreWithCompressedFile", "Error updating media store for ${mediaFile.fullPath}: ${e.message}")
                return false
            }
        }
        return false
    }


    companion object {

        /**
         * Create request to queue this worker to process
         */
        fun buildWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
//                .setRequiresDeviceIdle(true)  //TODO Provide support across OS versions
//                .setRequiresCharging(true)  //TODO
//                .setRequiresStorageNotLow(true)  //TODO
                .build()

            val workRequest = OneTimeWorkRequestBuilder<CompressionWorker>()
                .setConstraints(constraints)
                .build()

            return workRequest
        }

    }
}