package com.tmf.freespace.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerRequest(
    val requestType: ServerRequestType,
    val userID: String? = null,  //User ID GUID (REGISTER_USER, ALLOCATE_FILE, HEARTBEAT)
    val phoneNumber: String? = null,  //Phone number or unique ID of phone (REGISTER_USER)
    val email: String? = null,  //Email (REGISTER_USER)
    val password: String? = null,  //Password (REGISTER_USER)
    val fileID: Long? = null,  //File ID (from MediaStore)
    val fileSize: Int? = null,  //File size in bytes (REGISTER_USER, ALLOCATE_FILE)
    val sdSize: Long? = null,  //SD card size in bytes (REGISTER_USER)
    val sdFreeSpace: Long? = null,  //SD card free space at registration in bytes (REGISTER_USER)
) {
    enum class ServerRequestType(val value: Int) {
        REGISTER_USER(0),  //Register a new user (returns user ID and server credentials for client's pre-allocated (presumably 10MB) database backup file)
        ALLOCATE_FILE(1),  //Allocate file space on the FTP server pool
        HEARTBEAT(2),  //Client-to-server communication to log that client is still installed (no response)
        RECOVER_PASSWORD(3),  //Recover password (emails user a password reset link) (FUTURE)
        UPDATE_USER(4),  //Update user information (i.e. email, password) (FUTURE)
    }
}
