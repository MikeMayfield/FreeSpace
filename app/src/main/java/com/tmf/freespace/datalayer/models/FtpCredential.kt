package com.tmf.freespace.datalayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity()
data class FtpCredential(
    val serverID : Long = -1,  //Unique identifier for the FTP server    //Flags: -1 = Retry later; -2 = DB error
    val ipAddress: String,  //IP address of FTP server in dotted notation
    val username: String,  //FTP username
    val password: String , //FTP password
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
