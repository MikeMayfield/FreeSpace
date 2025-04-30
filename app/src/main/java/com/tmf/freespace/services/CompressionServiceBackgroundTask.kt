package com.tmf.freespace.services

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.tmf.freespace.MediaReader
import com.tmf.freespace.cloudstorage.CloudStorageFactory
import com.tmf.freespace.compression.Compressor
import com.tmf.freespace.database.AppDatabase
import com.tmf.freespace.files.MediaStoreUtil
import com.tmf.freespace.models.MediaFile
import java.io.File


//Perform background compression of media files for CompressionService
/*
. For all media files
. . Add new files to DB
. Determine amount of disk space to recover, based on user’s stated free space goal
. As SQL update query:
. . For all media, oldest to newest (> n days old), until sufficient free space available
. . . Choose compression level for file (based on algorithm below)
. . . If compression level changed from prior or no compression needed, but backup option selected
. . . . Update DB to request file processing later
. If any files to compress or recompress
. . Log into user account and get access token
. . For all media in DB with pending file processing requests
. . . Send file to cloud, if not already uploaded
. . . If file is already compressed
. . . . Restore file from cloud
. . . Compress file and replace original; update db
. . Update space used/available on server in db
 */

class CompressionServiceBackgroundTask(
    private val context: Context,
    private val database: AppDatabase
) {

    //region Properties and locals

    //endregion

    //region Public Methods

    //Start compression process. Must be called on background thread
    fun start()  {
        addAllNewMediaFilesToDB()  //Add all new media files to DB
        var bytesToRecover = getBytesToRecover()  //Determine amount of disk space to recover, based on user’s stated free space goal
        for (compressionLevelGroupIdx in 0..1) {  //If first pass doesn’t meet free space goal, try second pass with more aggressive compression
            if (bytesToRecover > 0L) {
                selectFilesToCompress(bytesToRecover, compressionLevelGroupIdx)  //Get all files to compress
                bytesToRecover = compressSelectedFiles(bytesToRecover)  //Compress all pending files
            }
        }

        if (bytesToRecover > 0L) {
            Log.w("start", "Not enough space was recovered. Remaining bytes: $bytesToRecover")
        }
    }

    //endregion

    //region Private Methods

    //Find all new media files on disk and add them to the database for future processing
    private fun addAllNewMediaFilesToDB() {
        val mediaFileReader = MediaReader(context)
        mediaFileReader.forNewMediaFiles{ mediaFile ->
            database.mediaFileDao.insertIfNew(mediaFile)
        }
    }

    //Calculate the amount of space to recover, based on user’s stated free space goal and the current free space on the device
    private fun getBytesToRecover(): Long {
        //Get current free space on primary disk
        val statFs = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val currentFreeSpace = statFs.availableBytes

        //Get desired free space from user preferences
        val desiredFreeSpace = currentFreeSpace + 10L  //TODO Get from preferences as (desiredFreeSpaceGB * 1GB)

        return desiredFreeSpace - currentFreeSpace
    }

    //Update the database for any files that should be compressed (or recompressed)
    private fun selectFilesToCompress(bytesToRecover: Long, compressionLevelGroupIdx: Int) {
        //Desired compression levels for images and videos, depending on their age
        val compressionLevels = listOf(
            listOf(  //Normal compression
                CompressionLevel(0, 31, 0, 0),  //No compression allowed
                CompressionLevel(31, 60, 1, 1),  //Image: Resolution 100% of screen, Compression 25%; Video: Screen resolution, Compression 25%
                CompressionLevel(60, 180, 2, 2),  //Image: Resolution 100% of screen, Compression 50%; Video: Resolution 720p (<=screen resolution), Compression 50%
                CompressionLevel(180, 365, 3, 3),  //Image: Resolution 50% of screen, Compression 75%; Video: Resolution 720p (<=screen), Compression 80%
                CompressionLevel(365, 10000, 4, 4),  //Image: Resolution 50% of screen, Compression 90%; Video: Resolution 480p (<=screen), Compression 90%
            ),
            listOf(  //Extra aggressive compression if normal compression was not enough
                CompressionLevel(0, 31, 0, 0),  //No compression allowed
                CompressionLevel(31, 60, 2, 2),  //Image: Resolution 100% of screen, Compression 50%; Video: Resolution 720p (<=screen), Compression 50%
                CompressionLevel(60, 180, 3, 3),  //Image: Resolution 50% of screen, Compression 80%; Video: Resolution 720p (<=screen), Compression 80%
                CompressionLevel(180, 365, 4, 4),  //Image: Resolution 50% of screen, Compression 90%; Video: Resolution 480p (<=screen), Compression 90%
                CompressionLevel(365, 10000, 5, 5),  //Image: Resolution 25% of screen, Compression 90%; Video: Resolution 320p (<=screen), Compression 90%
            ),
            //TODO: Add support for audio compression
        )

        val nowSecs = System.currentTimeMillis() / 1_000L
        val secondsPerDay: Long = 60 * 60 * 24
        for (compressionLevel in compressionLevels[compressionLevelGroupIdx]) {
            database.mediaFileDao.setCompressionLevels(nowSecs - compressionLevel.minDays * secondsPerDay, nowSecs - compressionLevel.maxDays * secondsPerDay,
                compressionLevel.imageCompressionLevel, compressionLevel.videoCompressionLevel)
        }
        //TODO Add support for optional full backup of all files
    }

    private fun compressSelectedFiles(bytesToRecover: Long) : Long {  //TODO Handle abort when no longer idle
        val mediaStoreUtil = MediaStoreUtil()
        val user = database.userDao.get()
        val cloudStorage = CloudStorageFactory().cloudStorage(user, context)
        var compressionRemainingBytes = bytesToRecover
        val compressor = Compressor(context)
        val filesToCompressCursor = database.mediaFileDao.getFilesToBeCompressed()
        var mediaFile: MediaFile? = database.mediaFileDao.nextMediaFile(filesToCompressCursor)
        while (mediaFile != null && compressionRemainingBytes > 0) {  //TODO Support optional backup of all files
            var extractedFilePath: String? = null

            //If file is not on server, send to cloud before it is compressed
            if (!mediaFile.isOnServer) {
                cloudStorage.sendMediaFile(mediaFile)  //TODO Send file to cloud async (coroutine), be sure compressing it while it is sending doesn't interfere with transfer and vice-versa
                mediaFile.isOnServer = true
            }

            //If file was already compressed, restore from cloud before recompressing
//TODO            if (mediaFile.currentCompressionLevel != 0) {
//                extractedFilePath = cloudStorage.restoreMediaFile(mediaFile)  //Restore media file from cloud to local file
//            }

            //Compress file
            val priorCompressedSize = mediaFile.compressedSize  //NOTE: compressedSize is initially the full file size before any compression
            val compressedFilePath = compressor.compress(mediaFile, extractedFilePath)
            if (compressedFilePath != null) {
                val compressedFile = File(compressedFilePath)
                if (compressedFile.exists() && compressedFile.length() < priorCompressedSize) {
                    if (mediaStoreUtil.replaceFileInMediaStore(context, mediaFile, compressedFilePath)) {
                        mediaFile.compressedSize = compressedFile.length().toInt()
                        mediaFile.currentCompressionLevel = mediaFile.desiredCompressionLevel
                        val bytesSaved = priorCompressedSize - mediaFile.compressedSize
                        compressionRemainingBytes -= bytesSaved
                        database.mediaFileDao.update(mediaFile)  //MediaFile has changed, so update the DB
                        Log.d("compressSelectedFiles", "Compressed ${mediaFile.displayName} from $priorCompressedSize to ${mediaFile.compressedSize} bytes. Remaining: $compressionRemainingBytes")
                    }
                }
            }

            //Go to the next file, if any
            mediaFile = database.mediaFileDao.nextMediaFile(filesToCompressCursor)
        }
        filesToCompressCursor.close()

        return compressionRemainingBytes
    }

    private data class CompressionLevel(
        val minDays: Int,
        val maxDays: Int,
        val imageCompressionLevel: Int,
        val videoCompressionLevel: Int,
    )

    //endregion
}