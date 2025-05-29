package com.tmf.freespace.models

data class ServerResponse(
    val responseType: ServerResponseType,
    val userID: Int,  //User ID (All)
    val serverID: Int,  //Server ID (Register, Allocate File)
    val ftpToken: String,  //FTP access token ((IpAsInt32 + space + login + space + password) XOR with userID, converted to UrlBase64 string)
) {
    enum class ServerResponseType(val value: Int) {
        REGISTERED(0),  //Registration or User update completed
        FILE_ACCESS(1),  //Allocate a file on one of the file servers
    }
}

