package com.tmf.freespace.services

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.tmf.freespace.MediaReader
import com.tmf.freespace.database.AppDatabase
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

class CompressFiles(
    private val context: Context,
    private val database: AppDatabase) {

    //region Properties and locals

    //endregion

    //region Public Methods

    //Start compression process. Must be called on IO coroutine dispatcher
    suspend fun start()  {
        addAllNewMediaFilesToDB()  //Add all new media files to DB
        val bytesToRecover = getBytesToRecover()  //Determine amount of disk space to recover, based on user’s stated free space goal
        if (bytesToRecover > 0L) {
            selectFilesToCompress(bytesToRecover)  //Get all files to compress
            val loginToken = loginToCloudServer()  //Log into user account and get access token
            compressFiles(loginToken)
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

    //Update the database for any files that should be compressed (more). Those files will be processed in the background, possibly multithreaded
    private fun selectFilesToCompress(bytesToRecover: Long) {
//        TODO("Not yet implemented")
    }

    private fun loginToCloudServer(): String {
        return ""  //TODO("Not yet implemented")
    }

    private suspend fun compressFiles(loginToken: String) {  //TODO Handle abort when no longer idle
        //Get list of files needing to be compressed or recompressed from db
        //For all files to compress (possibly batched)
        // . Send file to cloud, if not already uploaded
        // . If file is already compressed
        // . . Restore file from cloud
        // . Compress file and replace original; update db
        // . Update space used/available on server in db
        val filesToCompressCursor = database.mediaFileDao.getFilesToBeCompressed()
        var file: MediaFile? = database.mediaFileDao.nextMediaFile(filesToCompressCursor)
        while (file != null) {
            if (!file.isOnServer) {
                sendMediaFileToCloud(loginToken, file)
            }
            if (file.currentCompressionLevel != 0) {
                restoreMediaFileFromCloud(file)
            }
            compressMediaFile(file)

            file = database.mediaFileDao.nextMediaFile(filesToCompressCursor)
        }
    }

    private fun sendMediaFileToCloud(loginToken: String, file: MediaFile) {
//        TODO("Not yet implemented")
    }

    private fun restoreMediaFileFromCloud(file: MediaFile) {
//        TODO("Not yet implemented")
    }

    private fun compressMediaFile(file: MediaFile): Long {
//        TODO("Not yet implemented")
        return 10L  //TODO Return bytes compressed
    }

    //endregion
}