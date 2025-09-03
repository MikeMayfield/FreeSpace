package com.tmf.freespace.datalayer.datasources.cloudstorage

import User
import android.util.Log
import com.tmf.freespace.datalayer.models.FtpCredential
import com.tmf.freespace.datalayer.models.MediaFile
import java.io.File

class InterserverCloudStorage(val user: User) {
    private val ftpManager = FtpManager()

    suspend fun uploadMediaFile(mediaFile: MediaFile, sourceFilePath: String, ftpCredentials: FtpCredential) : Boolean {
        val sourceFile = File(sourceFilePath)
        if (!File(sourceFilePath).exists()) {
            Log.e("uploadMediaFile", "Source file does not exist: ${sourceFilePath}")
            return false
        }

        //Allocate space for file on FTP server
        if (ftpManager.login(ftpCredentials.hostAddress, 21, ftpCredentials.username, ftpCredentials.password)) {
            val remotePath = remotePath(mediaFile)
            if (ftpManager.uploadFile(sourceFile, "$remotePath.x")) {
                ftpManager.renameRemoteFile("$remotePath.x", remotePath)
                Log.d("uploadMediaFile", "File $remotePath uploaded successfully")
                return true
            }
        }
        ftpManager.close()

        return false
    }

    /**
     * Restore original file previously saved in the cloud
     *
     * @param mediaFile MediaFile to restore
     * @param outputFilePath Path to local file to create from cloud file
     * @return Flag: The file was restored successfully
     */
    suspend fun downloadMediaFile(mediaFile: MediaFile, outputFilePath: String, ftpCredentials: FtpCredential) : Boolean {
        if (ftpManager.login(ftpCredentials.hostAddress, 21, ftpCredentials.username, ftpCredentials.password)) {
            val result = ftpManager.downloadFile(remotePath(mediaFile), File(outputFilePath))
            ftpManager.close()
            return result  //TODO
        }
        return false
    }

    private fun remotePath(mediaFile: MediaFile) : String {
        return mediaFile.fullPath
    }

    private fun extractFileNameFromFullPath(fullPath: String) : String {
        return fullPath.substringAfterLast('/')
    }

}