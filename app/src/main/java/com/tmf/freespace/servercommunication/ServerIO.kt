package com.tmf.freespace.servercommunication

class ServerIO {
    //region Public API

    /**
     * Registers a user with the server.
     *
     * @param userID User ID GUID
     * @param phoneNumber The user's phone number, in the format "+1234567890".
     * @param emailAddress The user's email address.
     * @param sdSizeBytes The total size of the user's SD card in bytes
     * @param sdFreeSpaceBytes The amount of free space on the user's SD card in bytes.
     * @return Identical to allocateUploadFile
     */
    suspend fun registerUser(userID: String, phoneNumber: String, emailAddress: String, sdSizeBytes: Long, sdFreeSpaceBytes: Long) : String {
        return "1:1:1.2.3.4:user:password"  //TODO
    }

    /**
     * Request FTP access information for a file upload request in preparation for uploading the file to one of the FTP servers
     *
     * @param userID The user's unique identifier
     * @param fileID Local file ID (GUID); used to ID files by user on FTP servers for removal of orphaned files later
     * @param fileSizeBytes The size of the file being uploaded, in bytes.
     * @return File upload credentials, formatted as follows:
     *  versionNumber ("1") + ":" serverID ":" + IP.address.of.server + ":" + username + ":" + password
     *  Converted to a byte array, XORed with -1, converted to URL-encoded Base64
     *  ("*ERROR*_message" if error)
     */
    suspend fun allocateUploadFile(userID: String, fileID: String, fileSizeBytes: Long) : String {
        return "1:1:1.2.3.4:user:password"  //TODO
    }

    /**
     * Sends a heartbeat to the server to notify that the client is still installed
     *
     * @param userID The user's unique identifier.
     * @return None
     */
    suspend fun sendHeartBeat(userID: String) {
        //TODO
    }

    //endregion Public API
    //region Private methods

    internal fun decodeFtpToken(ftpCredentials: String) : String {
        return "TODO"
    }

    //endregion Private methods
}