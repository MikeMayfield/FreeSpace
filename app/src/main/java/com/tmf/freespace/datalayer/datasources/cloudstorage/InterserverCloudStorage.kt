package com.tmf.freespace.datalayer.datasources.cloudstorage

import android.util.Log
import com.tmf.freespace.datalayer.datasources.database.AppDatabase
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User
import com.tmf.freespace.datalayer.datasources.servercommunication.ServerIO
import java.io.File

class InterserverCloudStorage(val user: User, val database: AppDatabase) : ICloudStorage {
    private val serverIO = ServerIO(database)
    private val ftpManager = FtpManager()

    override suspend fun sendMediaFile(mediaFile: MediaFile, encoded: Boolean) : Boolean {
        val sourceFile = File(mediaFile.fullPath)
        if (!sourceFile.exists()) {
            Log.e("sendMediaFile", "Source file does not exist: ${mediaFile.fullPath}")
            return false
        }

        //Allocate space for file on FTP server
        val ftpCredentials = serverIO.allocateUploadFile(userIDGuid = user.idGuid, fileID = mediaFile.id, filename = mediaFile.fullPath, fileSizeBytes = mediaFile.originalSize)
        if (ftpCredentials != null && ftpManager.login(ftpCredentials.ipAddress, 21, ftpCredentials.username, ftpCredentials.password)) {
            val remotePath = remotePath(user, mediaFile)
            if (ftpManager.uploadFile(sourceFile, "$remotePath.x")) {  //TODO Add encoding support
                ftpManager.renameRemoteFile("$remotePath.x", extractFileNameFromFullPath(remotePath))
                mediaFile.serverID = ftpCredentials.serverID
                Log.d("sendMediaFile", "File $remotePath sent successfully")
                return true
            }
        }
        return false
    }

    /**
     * Restore original file previously saved in the cloud
     *
     * @param mediaFile MediaFile to restore
     * @param outputFilePath Path to local file to create from cloud file
     * @param encoded Flag: The file was encoded when saved
     * @return Flag: The file was restored successfully
     */
    override suspend fun restoreFileFromCloud(mediaFile: MediaFile, outputFilePath: String, encoded: Boolean) : Boolean {
        val ftpCredentials = database.ftpCredentialsDao.get(mediaFile.serverID)

        if (ftpManager.login(ftpCredentials.ipAddress, 21, ftpCredentials.username, ftpCredentials.password)) {
            val result = ftpManager.downloadFile(remotePath(user, mediaFile), File(outputFilePath))
            return result  //TODO
        }
        return false
    }

    override suspend fun close() {
        ftpManager.close()
    }

    private fun remotePath(user: User, mediaFile: MediaFile) : String {
        return "/${user.idGuid}${mediaFile.fullPath}"
    }

    private fun extractFileNameFromFullPath(fullPath: String) : String {
        return fullPath.substringAfterLast('/')
    }

}