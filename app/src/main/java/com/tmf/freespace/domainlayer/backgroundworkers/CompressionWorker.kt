package com.tmf.freespace.domainlayer.backgroundworkers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.tmf.freespace.datalayer.mediastore.MediaStoreUtil
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.domainlayer.compression.Compressor
import java.io.File
import androidx.core.net.toUri
import com.tmf.freespace.datalayer.models.MediaFile

/**
 * WorkManager task to compress (or re-compress) a file (Task #4)
 *
 * @param FileID ID of file to compress
 * @param UncompressedFileUri URI (as string) to uncompressed file to compress
 */
class CompressionWorker(val appContext: Context, val params: WorkerParameters): CoroutineWorker(appContext, params) {
    //TODO Add support for foreground service if working takes longer than almost 10 minutes to complete

    override suspend fun doWork(): Result {
        val fileID = inputData.getLong("FileID", 0)
        val uncompressedFileUri = inputData.getString("UncompressedFileUri")!!.toUri()
        val mediaFileRepository = MediaFileRepository(applicationContext)

        val mediaFile = mediaFileRepository.getMediaFileByID(fileID)
        if (mediaFile != null) {
            //Compress the uncompressed (original or recovered) media file
            val compressor = Compressor(applicationContext)
            val priorCompressedSize = mediaFile.compressedSize  //NOTE: compressedSize is initially the full file size before any compression
            val compressedFilePath = compressedFilePath(fileID)
            if (compressor.compress(mediaFile, "uncompressedFileUri", compressedFilePath)) {  //TODO Use extracted uncompressed file path
//                if (compressor.compress(mediaFile, uncompressedFileUri, compressedFilePath)) {  //TODO Use extracted uncompressed file path
                val compressedFile = File(compressedFilePath)
                if (compressedFile.length() < priorCompressedSize) {
                    //Update file in MediaStore with compressed file
                    updateMediaStoreWithCompressedFile(mediaFile, compressedFilePath)
                }
                deleteCompressedFile(compressedFilePath)
            }

            //Update DB to show compression processed (or not needed)
            mediaFile.currentCompressionLevel = mediaFile.desiredCompressionLevel  //Consider file compressed if compression was optimized out
            mediaFileRepository.updateMediaFile(mediaFile)

            //Delete temporary uncompressed file (if any)
            deleteUncompressedFile(uncompressedFileUri)
        }
        else {
            Log.e("compressSelectedFiles", "FileID $fileID not found in database")
        }

        return Result.success()
    }

    private fun deleteUncompressedFile(uncompressedFileUri: Uri) {
        if (uncompressedFileUri.scheme == "file") {
            val uncompressedFile = File(uncompressedFileUri.path!!)
            if (uncompressedFile.exists()) uncompressedFile.delete()
        }
    }

    private fun deleteCompressedFile(compressedFilePath: String) {
        val compressedFile = File(compressedFilePath)
        if (compressedFile.exists()) compressedFile.delete()  //Delete temporary compressed file
    }

    private fun compressedFilePath(id: Long): String {
        return "${appContext.cacheDir.absolutePath}/freespace/${id}.compressed"
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
//                .setRequiresCharging(true)
                .setRequiresStorageNotLow(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<CompressionWorker>()
                .setConstraints(constraints)
                .build()

            return workRequest
        }

    }
}