package com.tmf.freespace.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerRequest(
    val requestType: ServerRequestType? = ServerRequestType.REGISTER_USER,
    val userID: String? = null,  //User ID GUID (REGISTER_USER, ALLOCATE_FILE, HEARTBEAT)
    val phoneNumber: String? = null,  //Phone number or unique ID of phone (REGISTER_USER)
    val email: String? = null,  //Email (REGISTER_USER)
    val maxDiskSize: Long? = null,  //Maximum allowed space on FTP server, in gigabytes (REGISTER_USER, UPDATE_USER)
    val fileSize: Int? = null,  //File size in bytes (REGISTER_USER, ALLOCATE_FILE)
    val diskSize: Long? = null,  //SD card or FTP disk size in bytes (REGISTER_USER)
    val ftpCredentials: FtpCredential? = null,  //FTP access token (ALLOCATE_FILE)
) {
    enum class ServerRequestType(val value: Int) {
        REGISTER_USER(0),  //Register a new user (returns user ID and server credentials for client's pre-allocated (presumably 10MB) database backup file)
        ALLOCATE_FILE(1),  //Allocate file space on the FTP server pool
        HEARTBEAT(2),  //Client-to-server communication to log that client is still installed (no response)
        LOG_ERROR(3),  //Log error (FUTURE)
//        GET_DISK_INFO(4),  //Get disk information (FTP utility tool)
//        ADD_DISK(5),  //Add disk (FTP utility tool)
//        RECOVER_PASSWORD(6),  //Recover password (emails user a password reset link) (FUTURE)
    }

    /**
     * Register a new user.
     *
     * @param userID User ID GUID
     * @param phoneNumber Phone number or unique ID of phone
     * @param email Email
     * @param fileSize File size in bytes
     * @param diskSize SD card or FTP disk size in bytes
     * @return ServerRequest
     */
    fun registerUser(userID: String, phoneNumber: String, email: String, maxDiskSize: Long, fileSize: Int, diskSize: Long, sdFreeSpace: Long) : ServerRequest {
        return ServerRequest(ServerRequestType.REGISTER_USER, userID = userID, phoneNumber = phoneNumber, email = email,
            maxDiskSize = maxDiskSize, fileSize = fileSize, diskSize = diskSize)
    }

    /**
     * Allocate file space on the FTP server pool.
     *
     * @param userID User ID GUID
     * @param fileID File ID (from MediaStore)
     * @param fileSize File size in bytes
     * @return ServerRequest
     */
    fun allocateFile(userID: String, fileSize: Int) : ServerRequest {
        return ServerRequest(ServerRequestType.ALLOCATE_FILE, userID = userID, fileSize = fileSize)
    }

    /**
     * Send a heartbeat to the server to log that the client is still installed.
     *
     * @param userID User ID GUID
     * @return ServerRequest
     */
    fun heartbeat(userID: String) : ServerRequest {
        return ServerRequest(ServerRequestType.HEARTBEAT, userID = userID)
    }

//    /**
//     * Get information on all FTP disks
//     *
//     * @return ServerRequest
//     */
//    fun getDiskInfo() : ServerRequest {
//        return ServerRequest(ServerRequestType.GET_DISK_INFO)
//    }
//
//    /**
//     * Add a new FTP disk
//     *
//     * @param userID User ID GUID
//     * @param diskSize FTP disk size in bytes
//     * @param ftpCredentials FTP access token for new FTP server
//     * @return ServerRequest
//     */
//    fun addDisk(userID: String, diskSize: Long, ftpCredentials: FtpCredential) : ServerRequest {
//        return ServerRequest(ServerRequestType.ADD_DISK, userID = userID, diskSize = diskSize, ftpCredentials = ftpCredentials)
//    }
//
//    /**
//     * Recover password for existing user
//     *
//     * @param userID User ID GUID
//     * @param email Email
//     * @return ServerRequest
//     */
//    fun recoverPassword(userID: String, email: String) : ServerRequest {
//        return ServerRequest(ServerRequestType.RECOVER_PASSWORD, userID = userID, email = email)
//    }
//
//    /**
//     * Update user information
//     *
//     * @param userID User ID GUID
//     * @param email Email
//     * @param maxDiskSize Maximum allowed space on FTP server, in gigabytes
//     * @return ServerRequest
//     */
//    fun updateUser(userID: String, email: String, maxDiskSize: Long) : ServerRequest {
//        return ServerRequest(ServerRequestType.UPDATE_USER, userID = userID, email = email, maxDiskSize = maxDiskSize)
//    }
}
