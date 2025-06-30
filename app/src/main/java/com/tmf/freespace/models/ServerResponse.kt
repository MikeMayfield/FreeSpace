package com.tmf.freespace.models

//import kotlinx.serialization.Serializable
//
//@Serializable
//data class ServerResponse(
//    val responseType: ServerResponseType,
//    val ftpToken: String,  //FTP access token (('1:' + serverID + ':' + dotted.Ip.Address + ':' + login + ':' + password) XOR with -1, converted to UrlBase64 string)
//) {
//    enum class ServerResponseType(val value: Int) {
//        REGISTERED(0),  //Registration or User update completed
//        FILE_ACCESS(1),  //Allocated a file on one of the file servers
//        RETRY_LATER(2),  //Retry later (servers are busy or full)
//    }
//}
