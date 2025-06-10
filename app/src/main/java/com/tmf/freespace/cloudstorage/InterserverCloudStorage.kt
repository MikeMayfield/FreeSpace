package com.tmf.freespace.cloudstorage

import android.content.Context
import com.tmf.freespace.database.AppDatabase
import com.tmf.freespace.ftpclient.FtpManager
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User
import com.tmf.freespace.servercommunication.ServerIO
import java.io.File

class InterserverCloudStorage(val user: User, context: Context) : ICloudStorage {
    private val serverIO = ServerIO()
    private val ftpManager = FtpManager()
    private val database = AppDatabase(context)

    override suspend fun sendMediaFile(mediaFile: MediaFile, encoded: Boolean) {
        val sourceFile = File(mediaFile.fullPath)
        if (!sourceFile.exists()) {
            throw NoSuchFileException(sourceFile)
        }

        //Allocate space for file on FTP server
        val ftpCredentials = serverIO.allocateUploadFile(userIDGuid = user.idGuid, fileID = mediaFile.id, fileSizeBytes = mediaFile.originalSize)

        if (ftpManager.login(ftpCredentials.ipAddress, 21, ftpCredentials.username, ftpCredentials.password)) {
            val remotePath = remotePath(user, mediaFile)
            if (ftpManager.uploadFile(sourceFile, "$remotePath.tmp")) {  //TODO Add encoding support
                ftpManager.renameRemoteFile("$remotePath.tmp", remotePath)
                mediaFile.serverID = ftpCredentials.serverID
            }
        }
    }

    /**
     * Restore original file previously saved in the cloud
     *
     * @param mediaFile MediaFile to restore
     * @param outputPath Path to local file to create from cloud file
     * @param encoded Flag: The file was encoded when saved
     * @return Flag: The file was restored successfully
     */
    override suspend fun restoreMediaFile(mediaFile: MediaFile, outputFilePath: String, encoded: Boolean) : Boolean {
        val ftpCredentials = database.ftpCredentialsDao.get(mediaFile.serverID)

        if (ftpManager.login(ftpCredentials.ipAddress, 21, ftpCredentials.username, ftpCredentials.password)) {
            val remotePath = remotePath(user, mediaFile)
            if (ftpManager.downloadFile("$remotePath.rmt", File(outputFilePath))) {  //TODO Add encoding support
                ftpManager.renameRemoteFile("$remotePath.tmp", remotePath)
                mediaFile.serverID = ftpCredentials.serverID
            }
            return true
        }

        return false
    }

    override suspend fun close() {
        ftpManager.close()
    }

    private fun remotePath(user: User, mediaFile: MediaFile) : String {
        return "Freespace/${user.idGuid}/${mediaFile.id}.rmt"
    }
}