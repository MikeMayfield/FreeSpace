package com.tmf.freespace.datalayer.datasources.network

import com.tmf.freespace.datalayer.models.FtpCredential
import java.util.UUID

class ServerIO() {
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
     * Allocate file space and return FTP access information for a file upload request in preparation for uploading the file to one of the FTP servers
     *
     * @param userIDGuid The user's unique identifier
     * @param fileID Local file ID (GUID); used to ID files by user on FTP servers for removal of orphaned files later
     * @param filename The name (full path) of the file being uploaded
     * @param fileSizeBytes The size of the file being uploaded, in bytes.
     * @return File upload credentials, formatted as follows:
     */
    fun allocateFileInCloud(userIDGuid: UUID, fileID: Long, filename: String, fileSizeBytes: Int) : FtpCredential? {
        var ftpCredential: FtpCredential? = null

        try {
            //TODO Get FTP credentials from server
            val ftpCredentialsFromServer = "1:1:66.45.241.246:st60470:9N2mxY@V"  //TODO

            ftpCredential = decodeFtpToken(ftpCredentialsFromServer)
        }
        catch (e: Exception) {
            //TODO Handle error getting credentials from server
        }

        return ftpCredential
    }

    fun getFtpCredentials(userIDGuid: UUID, fileID: Long) : FtpCredential? {
        //TODO Get FTP credentials from server
        val ftpCredentialsFromServer = "1:1:66.45.241.246:st60470:9N2mxY@V"  //TODO

        return decodeFtpToken(ftpCredentialsFromServer)
    }

    /**
     * Sends a heartbeat to the server to notify that the client is still installed
     *
     * @param userIDGuid The user's unique identifier.
     * @return None
     */
    suspend fun sendHeartBeat(userIDGuid: String) {
        TODO();
    }

    //endregion Public API
    //region Private methods

    /**
     * Decode FTP credentials from server
     *
     * Format of FTP credentials token is:
     *  versionNumber ("1") + ":" serverID ":" + IP.address.of.server + ":" + username + ":" + password
     *  Converted to a byte array, XORed with -1, converted to URL-encoded Base64
     *  ("*ERROR*_message" if error)     */
    private fun decodeFtpToken(token: String) : FtpCredential {
        val decodedString = token  //TODO Decode string when sent from server

//        // 1. Convert from UrlBase64/Standard Base64 to byte array
//        //    Assuming standard Base64 here. Use Base64.URL_SAFE if applicable.
//        val decodedBytes: ByteArray
//        try {
//            decodedBytes = Base64.decode(source = token)
//        } catch (e: IllegalArgumentException) {
//            System.err.println("FTP Token is not valid Base64: ${e.message}")
//            throw IllegalArgumentException("Token is not valid Base64", e)
//        }
//
//        // 2. Decode byte array by XORing with -1 and converting to string (assuming UTF-8)
//        decodedBytes.forEachIndexed { index, byte ->
//            decodedBytes[index] = (byte.toInt() xor -1).toByte()
//        }
//        val decodedString = String(decodedBytes, StandardCharsets.UTF_8)

        // 3. Split tokens
        //    Assuming the format is "VERSION:IP_ADDRESS:USERNAME:PASSWORD"
        //    Adjust the delimiter and order as needed.
        val parts = decodedString.split(":", limit = 5) // limit = 5 to ensure password can contain colons
        if (parts.size < 5) { // Or whatever minimum number of parts you expect
            throw IllegalArgumentException("Invalid token structure after decoding. Expected at least 4 parts, got ${parts.size}.")
        }

        // 4. Verify version number (optional but recommended)
        val versionStr = parts[0]
        try {
            val version = versionStr.toInt()
            if (version != 1) { // Assuming current version is 1
                throw IllegalArgumentException("Unsupported token version: $version")
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid version number in token: $versionStr", e)
        }

        // 5. Update fields. Assign parts based on defined order
        return FtpCredential(
            serverID = parts[1].toLong(),
            ipAddress = parts[2],
            username = parts[3],
            password = parts[4],
        )
    }

    //endregion Private methods
}