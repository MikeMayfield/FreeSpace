package com.tmf.freespace.datalayer.datasources.cloudstorage

import android.util.Log
import com.tmf.freespace.datalayer.datasources.database.AppDatabase
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User
import com.tmf.freespace.datalayer.datasources.network.ServerIO
import java.io.File

class InterserverCloudStorage(val user: User, val database: AppDatabase) : ICloudStorage {
    private val serverIO = ServerIO(database)
    private val ftpManager = FtpManager()

    override suspend fun uploadMediaFile(mediaFile: MediaFile, encoded: Boolean) : String? {
        val sourceFile = File(mediaFile.fullPath)
        if (!sourceFile.exists()) {
            Log.e("uploadMediaFile", "Source file does not exist: ${mediaFile.fullPath}")
            return null
        }

        //Allocate space for file on FTP server
        val ftpCredentials = serverIO.allocateUploadFile(userIDGuid = user.idGuid, fileID = mediaFile.id, filename = mediaFile.fullPath, fileSizeBytes = mediaFile.originalSize)
        if (ftpCredentials != null && ftpManager.login(ftpCredentials.ipAddress, 21, ftpCredentials.username, ftpCredentials.password)) {
            val remotePath = remotePath(user, mediaFile)
            if (ftpManager.uploadFile(sourceFile, "$remotePath.x")) {  //TODO Add encoding support
                ftpManager.renameRemoteFile("$remotePath.x", extractFileNameFromFullPath(remotePath))
                mediaFile.serverID = ftpCredentials.serverID
                Log.d("uploadMediaFile", "File $remotePath sent successfully")
                return remotePath
            }
        }
        return null
    }

    /**
     * Restore original file previously saved in the cloud
     *
     * @param mediaFile MediaFile to restore
     * @param outputFilePath Path to local file to create from cloud file
     * @param encoded Flag: The file was encoded when saved
     * @return Flag: The file was restored successfully
     */
    override suspend fun downloadMediaFile(mediaFile: MediaFile, outputFilePath: String, encoded: Boolean) : Boolean {
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