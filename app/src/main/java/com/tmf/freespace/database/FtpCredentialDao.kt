package com.tmf.freespace.database

import com.tmf.freespace.models.CloudStorageType
import com.tmf.freespace.models.FtpCredential
import com.tmf.freespace.models.User
import java.util.UUID

class FtpCredentialDao(private val database: AppDatabase) {
    private val tableName = "FtpCredential"

    //    @Query("SELECT * FROM FtpCredential WHERE ServerID=serverID")
    fun get(serverID: Int): FtpCredential {
        database.read.rawQuery("SELECT * FROM FtpCredential WHERE ServerID=$serverID;", null).use { cursor ->
            if (cursor.moveToFirst()) {
                return FtpCredential.fromCursor(cursor)!!
            } else {
                return FtpCredential("1:-1:0.0.0.0::")
            }
        }
    }
}