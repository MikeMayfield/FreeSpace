package com.tmf.freespace.servercommunication

import com.tmf.freespace.database.AppDatabase
import com.tmf.freespace.models.FtpCredential
import java.util.UUID

class ServerIO(val database: AppDatabase) {
    //region Public API

    /**
     * Registers a user with the server.
     *
     * @param userIDGuid User ID GUID
     * @param phoneNumber The user's phone number, in the format "+1234567890".
     * @param emailAddress The user's email address.
     * @param sdSizeBytes The total size of the user's SD card in bytes
     * @param sdFreeSpaceBytes The amount of free space on the user's SD card in bytes.
     * @return Identical to allocateUploadFile
     */
//    suspend fun registerUser(userIDGuid: String, phoneNumber: String, emailAddress: String, sdSizeBytes: Long, sdFreeSpaceBytes: Long) : String {
//        return "1:1:66.45.241.246:st60470:9N2mxY@V"  //TODO
//    }

    /**
     * Request FTP access information for a file upload request in preparation for uploading the file to one of the FTP servers
     *
     * @param userIDGuid The user's unique identifier
     * @param fileID Local file ID (GUID); used to ID files by user on FTP servers for removal of orphaned files later
     * @param fileSizeBytes The size of the file being uploaded, in bytes.
     * @return File upload credentials, formatted as follows:
     *  versionNumber ("1") + ":" serverID ":" + IP.address.of.server + ":" + username + ":" + password
     *  Converted to a byte array, XORed with -1, converted to URL-encoded Base64
     *  ("*ERROR*_message" if error)
     */
    fun allocateUploadFile(userIDGuid: UUID, fileID: Long, fileSizeBytes: Int) : FtpCredential? {
        var ftpCredential: FtpCredential? = null

        try {
            //TODO Get FTP credentials from server
            val ftpCredentialsFromServer = "1:1:66.45.241.246:st60470:9N2mxY@V"

            ftpCredential = decodeFtpToken(ftpCredentialsFromServer)
            database.ftpCredentialsDao.insertIfNew(ftpCredential)
        }
        catch (e: Exception) {
            //TODO Handle error getting credentials from server
        }

        return ftpCredential
    }

    /**
     * Sends a heartbeat to the server to notify that the client is still installed
     *
     * @param userIDGuid The user's unique identifier.
     * @return None
     */
//    suspend fun sendHeartBeat(userIDGuid: String) {
//        //TODO
//    }

    //endregion Public API
    //region Private methods

    internal fun decodeFtpToken(ftpCredentials: String) : FtpCredential {
        return FtpCredential(ftpCredentials)
    }

    //endregion Private methods
}