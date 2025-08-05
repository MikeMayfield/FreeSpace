package com.tmf.freespace.datalayer.datasources.database

import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import com.tmf.freespace.models.FtpCredential

class FtpCredentialDao(private val database: AppDatabase) {
    private val tableName = "FtpCredential"

    //Insert record if it doesn't already exist (based on ServerID)
    fun insertIfNew(ftpCredential: FtpCredential) {
        database.write.insertWithOnConflict(tableName, null, ftpCredential.getContentValues(), CONFLICT_IGNORE)
    }

    //    @Query("SELECT * FROM FtpCredential WHERE ServerID=serverID")
    fun get(serverID: Int): FtpCredential {
        database.read.rawQuery("SELECT * FROM FtpCredential WHERE ServerID=$serverID;", null).use { cursor ->
            return if (cursor.moveToFirst()) {
                FtpCredential.fromCursor(cursor)
            } else {
                FtpCredential("1:1:66.45.241.246:st60470:9N2mxY@V")
            }
        }
    }
}