//package com.tmf.freespace.uilayer.backgroundworkers
//
//import android.content.Context
//import android.os.Environment
//import android.os.StatFs
//import android.util.Log
//import com.tmf.freespace.MediaReader
//import com.tmf.freespace.datalayer.datasources.cloudstorage.CloudStorageFactory
//import com.tmf.freespace.datalayer.datasources.cloudstorage.ICloudStorage
//import com.tmf.freespace.domainlayer.compression.Compressor
//import com.tmf.freespace.datalayer.datasources.database.AppDatabase
//import com.tmf.freespace.datalayer.mediastore.MediaStoreUtil
//import com.tmf.freespace.models.MediaFile
//import java.io.File
//import kotlin.io.path.createTempFile
//
//
////Perform background compression of media files for CompressionService
///*
//. For all media files
//. . Add new files to DB
//. Determine amount of disk space to recover, based on user’s stated free space goal
//. As SQL update query:
//. . For all media, oldest to newest (> n days old), until sufficient free space available
//. . . Choose compression level for file (based on algorithm below)
//. . . If compression level changed from prior or no compression needed, but backup option selected
//. . . . Update DB to request file processing later
//. If any files to compress or recompress
//. . Log into user account and get access token
//. . For all media in DB with pending file processing requests
//. . . Send file to cloud, if not already uploaded
//. . . If file is already compressed
//. . . . Restore file from cloud
//. . . Compress file and replace original; update db
//. . Update space used/available on server in db
//*/
//class CompressionServiceBackgroundTask(
//    private val context: Context,
//    private val database: AppDatabase
//) {
//
//    //region Properties and locals
//
//    //Desired compression levels for images and videos, depending on their age
//    private val compressionLevels = listOf(
//        listOf(  //Normal compression
//            CompressionLevel(0, 31, 2, 1),  //No compression allowed  //TODO change to 0s
//            CompressionLevel(31, 60, 1, 1),  //Image: Resolution 100% of screen, Compression 25%; Video: Screen resolution, Compression 25%
//            CompressionLevel(60, 180, 2, 2),  //Image: Resolution 100% of screen, Compression 50%; Video: Resolution 720p (<=screen resolution), Compression 50%
//            CompressionLevel(180, 365, 3, 3),  //Image: Resolution 50% of screen, Compression 75%; Video: Resolution 720p (<=screen), Compression 80%
//            CompressionLevel(365, 10000, 4, 4),  //Image: Resolution 50% of screen, Compression 90%; Video: Resolution 480p (<=screen), Compression 90%
//        ),
//        listOf(  //Extra aggressive compression if normal compression was not enough
//            CompressionLevel(0, 31, 0, 0),  //No compression allowed
//            CompressionLevel(31, 60, 2, 2),  //Image: Resolution 100% of screen, Compression 50%; Video: Resolution 720p (<=screen), Compression 50%
//            CompressionLevel(60, 180, 3, 3),  //Image: Resolution 50% of screen, Compression 80%; Video: Resolution 720p (<=screen), Compression 80%
//            CompressionLevel(180, 365, 4, 4),  //Image: Resolution 50% of screen, Compression 90%; Video: Resolution 480p (<=screen), Compression 90%
//            CompressionLevel(365, 10000, 5, 5),  //Image: Resolution 25% of screen, Compression 90%; Video: Resolution 320p (<=screen), Compression 90%
//        ),
//        //TODO: Add support for audio compression
//    )
//
//    private lateinit var extractedFilePath: String  //ExtractedFile.tmp on ExternalStorageDirectory storage (used for extracting media file from storage or restoring media from server
//    private lateinit var compressedFilePath: String  //CompressedFile.tmp on ExternalStorageDirectory storage (used for extracting media file from storage or restoring media from server
//    private lateinit var cloudStorage: ICloudStorage
//    private lateinit var compressor: Compressor
//    private val mediaStoreUtil = MediaStoreUtil()
//    val user = database.userDao.get()
//
//    //endregion
//
//    //region Public Methods
//
//    //Start compression process. Must be called on background thread
//    suspend fun start()  {
//        extractedFilePath = createTempFile(prefix = "Extract_", suffix = ".tmp").toString()
//        compressedFilePath = createTempFile(prefix = "Compress_", suffix = ".tmp").toString()
//        cloudStorage = CloudStorageFactory().cloudStorage(user, context, database)
//        compressor = Compressor(context)
//
//        addAllNewMediaFilesToDB()  //Add all new media files to DB
//        var bytesToRecover = calculateBytesToRecover()  //Determine amount of disk space to recover, based on user’s stated free space goal
//        for (compressionLevelGroupIdx in compressionLevels.indices) {  //If first pass doesn’t meet free space goal, try second pass with more aggressive compression
//            if (bytesToRecover > 0L) {
//                selectFilesToCompress(compressionLevelGroupIdx)  //Get all files to compress
//                bytesToRecover -= compressSelectedFiles(bytesToRecover)  //Compress all pending files
//            }
//        }
//
//        if (bytesToRecover > 0L) {
//            Log.w("start", "Not enough space was recovered. Remaining bytes: $bytesToRecover")
//            //TODO Compress again with more aggressive settings
//        }
//    }
//
//    //endregion
//
//    //region Private Methods
//
//    //Find all new media files on disk and add them to the database for future processing
//    fun addAllNewMediaFilesToDB() {
//        val mediaFileReader = MediaReader(context)
//        mediaFileReader.forNewMediaFiles{ mediaFile ->
//            database.mediaFileDao.insertIfNew(mediaFile)
//        }
//    }
//
//    //Calculate the amount of space to recover, based on user’s stated free space goal and the current free space on the device
//    fun calculateBytesToRecover(): Long {
//        //Get current free space on primary disk
//        val statFs = StatFs(Environment.getExternalStorageDirectory().absolutePath)
//        val currentFreeSpace = statFs.availableBytes
//
//        //Get desired free space from user preferences
//        val desiredFreeSpace = currentFreeSpace + 1000_000_000L  //TODO Get from preferences as (desiredFreeSpaceGB * 1GB)
//
//        return desiredFreeSpace - currentFreeSpace
//    }
//
//    //Update the database for any files that should be compressed (or recompressed)
//    private fun selectFilesToCompress(compressionLevelGroupIdx: Int) {
//
//        val nowSecs = System.currentTimeMillis() / 1_000L
//        val secondsPerDay: Long = 60 * 60 * 24
//        for (compressionLevel in compressionLevels[compressionLevelGroupIdx]) {
//            database.mediaFileDao.setCompressionLevels(nowSecs - compressionLevel.minDays * secondsPerDay, nowSecs - compressionLevel.maxDays * secondsPerDay,
//                compressionLevel.imageCompressionLevel, compressionLevel.videoCompressionLevel)
//        }
//        //TODO Add support for optional full backup of all files
//    }
//
//    /**
//     * Compress all files that should be compressed (or recompressed)
//     *
//     * @param bytesToRecover The amount of space to recover
//     * @return The amount of space that still needs to be recovered after compressing all files (<=0 if no more recovery needed)
//     */
//    private suspend fun compressSelectedFiles(bytesToRecover: Long) : Long {  //TODO Handle abort when no longer idle
//        var bytesRemainingToRecover = bytesToRecover
//        val compressor = Compressor(context)
//        val filesToCompressCursor = database.mediaFileDao.getFilesToBeCompressed()
//        var mediaFile: MediaFile? = database.mediaFileDao.nextMediaFile(filesToCompressCursor)
//        while (mediaFile != null && bytesRemainingToRecover > 0) {  //TODO Support optional backup of all files
//            //TODO Check for enough free space to store extracted file twice (once for original, once for compressed)
//
//            if (fileExists(mediaFile)) {
//                var okToCompressFile = true
//
//                //If file is not already on server, send to cloud before it is compressed (TODO: Send to cloud async while compressing)
//                if (!mediaFile.isOnServer) {
//                    if (!cloudStorage.uploadMediaFile(mediaFile)) {  //TODO Send file to cloud async (coroutine). Be sure compressing it while it is sending doesn't interfere with transfer and vice-versa
//                        Log.w("compressSelectedFiles", "Failed to send file to cloud: ${mediaFile.displayName}. Not compressing")
//                        //TODO Send diagnostic info to server
//                        okToCompressFile = false
//                    }
//                }
//
//                //Compress (or recompress) the file if it is eligible
//                if (okToCompressFile) {
//                    val uncompressedFilePath = extractOrRecoverMediaFile(mediaFile)
//                    if (uncompressedFilePath != null) {
//                        val priorFileSize = mediaFile.compressedSize
//                        if (compressor.compress(mediaFile, uncompressedFilePath, compressedFilePath)) {
//                            if (updateMediaStoreWithCompressedFile(mediaFile, compressedFilePath)) {
//                                bytesRemainingToRecover -= (priorFileSize - mediaFile.compressedSize)
//                                Log.d("compressSelectedFiles", "Compressed ${mediaFile.fullPath} from {$priorFileSize} to ${mediaFile.compressedSize} bytes")
//                            }
//                            else {
//                                Log.w("compressSelectedFiles", "Not enough space recovered from compression to update media store for ${mediaFile.displayName}")
//                            }
//                        }
//                        else {
//                            mediaFile.currentCompressionLevel = mediaFile.desiredCompressionLevel  //Consider file compressed if compression was optimized out
//                        }
//                    }
//                }
//
//                deleteTempFiles()
//            }
//            database.mediaFileDao.update(mediaFile)  //MediaFile has (probably) changed, so update the DB
//
//            //Go to the next file, if any
//            mediaFile = database.mediaFileDao.nextMediaFile(filesToCompressCursor)
//        }
//        filesToCompressCursor.close()
//        cloudStorage.close()
//
//        return bytesRemainingToRecover
//    }
//
//    private fun fileExists(mediaFile: MediaFile): Boolean {
//        if (File(mediaFile.fullPath).exists()) {
//            return true
//        }
//        else {
//            Log.w("compressSelectedFiles", "File no longer exists: ${mediaFile.displayName}")
//            //Ignore this file in the future
//            mediaFile.compressedSize = 0
//            mediaFile.currentCompressionLevel = 0
//            mediaFile.desiredCompressionLevel = 0
//            database.mediaFileDao.update(mediaFile)
//            return false
//        }
//    }
//
//    private suspend fun extractOrRecoverMediaFile(mediaFile: MediaFile): String? {
//        //Extract media file from local media store or from cloud, if previously sent to cloud
//        if (mediaFile.currentCompressionLevel == 0) {  //Never compressed, so original version is in media store
//            Log.d("extractOrRecoverMediaFile", "Using ${mediaFile.fullPath} from MediaStore")
//            return mediaFile.fullPath  //Path to file in Media Store
//        }
//        else {  //If file was already compressed, restore from cloud before recompressing
//            if (cloudStorage.downloadMediaFile(mediaFile, extractedFilePath)) {  //Restore media file from cloud to local file
//                Log.d("extractOrRecoverMediaFile", "Restored ${mediaFile.fullPath} from cloud")
//                return extractedFilePath
//            }
//            else {
//                Log.e("CompressSelectedFiles:", "Failed to restore original file to be recompressed: ${mediaFile.id}: ${mediaFile.displayName}")
//                mediaFile.serverID = -1  //Force it to upload next time it's processed
//                return null
//            }
//        }
//    }
//
//    private fun updateMediaStoreWithCompressedFile(mediaFile: MediaFile, compressedFilePath: String) : Boolean {
//        val priorCompressedSize = mediaFile.compressedSize  //NOTE: compressedSize is initially the full file size before any compression
//        val compressedFile = File(compressedFilePath)
//        if (compressedFile.exists()) {
//            if (compressedFile.length() < (priorCompressedSize - compressor.minSignificantCompressionBytes)) {
//                //If compressed file is smaller than prior version, replace media file with compressed file
//                try {
//                    if (mediaStoreUtil.overwriteMediaStoreFile(context, mediaFile, compressedFilePath)) {
//                        mediaFile.compressedSize = compressedFile.length().toInt()
//                        mediaFile.currentCompressionLevel = mediaFile.desiredCompressionLevel
//                        Log.d("updateMediaStoreWithCompressedFile", "Updated media store for ${mediaFile.displayName} from $priorCompressedSize to ${mediaFile.compressedSize} bytes")
//                        return true
//                    }
//                } catch (e: Exception) {
//                    Log.e("updateMediaStoreWithCompressedFile", "Error updating media store for ${mediaFile.displayName}: ${e.message}")
//                    return false
//                }
//            }
//        }
//        return false
//    }
//
//    private fun deleteTempFiles() {
//        val extractedFile = File(extractedFilePath)
//        if (extractedFile.exists()) extractedFile.delete()
//
//        val compressedFilePath = File(compressedFilePath)
//        if (compressedFilePath.exists()) compressedFilePath.delete()
//    }
//
//    //endregion
//
//
//    data class CompressionLevel(
//        val minDays: Int,
//        val maxDays: Int,
//        val imageCompressionLevel: Int,
//        val videoCompressionLevel: Int = imageCompressionLevel,
//    )
//}