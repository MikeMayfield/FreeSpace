package com.tmf.freespace.services

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.tmf.freespace.MediaReader
import com.tmf.freespace.cloudstorage.CloudStorageFactory
import com.tmf.freespace.database.AppDatabase
import com.tmf.freespace.models.CloudStorageType
import com.tmf.freespace.models.MediaFile


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

class CompressionServiceHelper(
    private val context: Context,
    private val database: AppDatabase
) {

    //region Properties and locals

    //endregion

    //region Public Methods

    //Start compression process. Must be called on background thread
    fun start()  {
        addAllNewMediaFilesToDB()  //Add all new media files to DB
        val bytesToRecover = getBytesToRecover()  //Determine amount of disk space to recover, based on user’s stated free space goal
        if (bytesToRecover > 0L) {
            selectFilesToCompress(bytesToRecover)  //Get all files to compress
            compressSelectedFiles(bytesToRecover)  //Compress all pending files
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
    private fun selectFilesToCompress(bytesToRecover: Long) {
        //Desired compression levels for images and videos, depending on their age
        val compressionLevels = listOf(
            CompressionLevel(0, 31, 0, 0),  //No compression allowed
            CompressionLevel(31, 60, 100, 0),  //Image: Resolution 100% of screen, Compression 80%
            CompressionLevel(60, 180, 200, 250),  //Image: Resolution 100% of screen, Compression 50%; Video: Resolution 720p, Compression 50%
            CompressionLevel(180, 365, 300, 350),  //Image: Resolution 50% of screen, Compression 80%; Video: Resolution 720p, Compression 80%
            CompressionLevel(365, 10000, 400, 450),  //Image: Resolution 50% of screen, Compression 90%; Video: Resolution 480p, Compression 90%
            //TODO Add more compression level(s) in case normal compression is not enough after a pass
        )

        val nowSecs = System.currentTimeMillis() / 1000L
        Log.d("CompressFiles", "Now: $nowSecs")
        val secondsPerDay: Long = 60 * 60 * 24
        for (compressionLevel in compressionLevels) {
            Log.d("CompressFiles", "Min: ${compressionLevel.minDays} ${nowSecs - compressionLevel.minDays * secondsPerDay}, Max: ${compressionLevel.maxDays} ${nowSecs - compressionLevel.maxDays * secondsPerDay}")
            database.mediaFileDao.setCompressionLevel(nowSecs - compressionLevel.minDays * secondsPerDay, nowSecs - compressionLevel.maxDays * secondsPerDay,
                compressionLevel.imageCompressionLevel, compressionLevel.videoCompressionLevel)
        }
    }

    private fun loginToCloudServer(): String {
        return ""  //TODO("Not yet implemented")
    }

    private fun compressSelectedFiles(bytesToRecover: Long) {  //TODO Handle abort when no longer idle
        //Get list of files needing to be compressed or recompressed from db
        //For all files to compress (possibly batched)
        // . Send file to cloud, if not already uploaded
        // . If file is already compressed
        // . . Restore file from cloud
        // . Compress file and replace original; update db
        // . Update space used/available on server in db
        var cloudStorage = CloudStorageFactory().CloudStorage(CloudStorageType.Simulated)  //TODO use user-selected cloud storage from User record
        var compressionRemainingBytes = bytesToRecover
        val loginToken = loginToCloudServer()  //Log into user account and get access token
        val filesToCompressCursor = database.mediaFileDao.getFilesToBeCompressed()
        var file: MediaFile? = database.mediaFileDao.nextMediaFile(filesToCompressCursor)
        while (file != null && compressionRemainingBytes > 0) {
            //If file is not on server, send to cloud before it is compressed
            if (!file.isOnServer) {
                sendMediaFileToCloud(loginToken, file)
                file.isOnServer = true
            }

            //If file is already compressed, restore from cloud before recompressing
            if (file.currentCompressionLevel != 0) {
                restoreMediaFileFromCloud(loginToken, file)
            }

            //Compress file
            val compressedSize = compressMediaFile(file)
            file.compressedSize = compressedSize
            file.currentCompressionLevel = file.desiredCompressionLevel
            val bytesSaved = file.originalSize - compressedSize
            compressionRemainingBytes -= bytesSaved

            //MediaFile has changed, so update the DB
//TODO            database.mediaFileDao.update(file)

            //Go to the next file, if any
            file = database.mediaFileDao.nextMediaFile(filesToCompressCursor)
        }
        filesToCompressCursor.close()

        //TODO In the future we might want to provide a more aggressive compression selection if first pass doesn't meet free space goal
        if (compressionRemainingBytes > 0) {
            Log.w("compressSelectedFiles", "Not enough space was recovered. Remaining bytes: $compressionRemainingBytes")
        }
    }

    private fun sendMediaFileToCloud(loginToken: String, file: MediaFile) {
//        TODO("Not yet implemented")
    }

    private fun restoreMediaFileFromCloud(loginToken: String, file: MediaFile) {
//        TODO("Not yet implemented")
    }

    private fun compressMediaFile(file: MediaFile): Int {
        return (file.originalSize.toFloat() * 0.5f).toInt()  //TODO Return bytes compressed
    }

    private data class CompressionLevel(
        val minDays: Int,
        val maxDays: Int,
        val imageCompressionLevel: Int,
        val videoCompressionLevel: Int,
    )

    //endregion
}