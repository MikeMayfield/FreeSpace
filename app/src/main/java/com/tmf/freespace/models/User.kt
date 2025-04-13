package com.tmf.freespace.models

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.tmf.freespace.database.AppDatabase
import java.util.UUID

data class User(
    var id: Long,
    val idGuid: UUID = UUID.randomUUID(),
    val phoneNumber: String,
    val emailAddress: String,
    val password: String,
    var maxExpansionAllowed: Int,
    var cloudStorageType: CloudStorageType,
) {
    fun getContentValues(excludeId: Boolean = false) : ContentValues {
        return ContentValues().apply {
            put("id", id)
            put("idGuid", idGuid.toString())
            put("phoneNumber", phoneNumber)
            put("emailAddress", emailAddress)
            put("password", password)
            put("maxExpansionAllowed", maxExpansionAllowed)
            put("cloudStorageType", cloudStorageType.toString())
        }
    }

    companion object {
        fun fromCursor(cursor: Cursor) : User? {
            if (cursor.moveToNext()) {
                return User(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    idGuid = UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow("idGuid"))),
                    phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber")),
                    emailAddress = cursor.getString(cursor.getColumnIndexOrThrow("emailAddress")),
                    password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
                    maxExpansionAllowed = cursor.getInt(cursor.getColumnIndexOrThrow("maxExpansionAllowed")),
                    cloudStorageType = CloudStorageType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("cloudStorageType"))),
                )
            }
            else {
                return null
            }
        }

        fun createTable() : String {
            val sb = StringBuilder("CREATE TABLE IF NOT EXISTS User (")
            sb.append("id INTEGER PRIMARY KEY AUTOINCREMENT, ")
            sb.append("idGuid TEXT NOT NULL, ")
            sb.append("phoneNumber TEXT NOT NULL, ")
            sb.append("emailAddress TEXT NOT NULL, ")
            sb.append("password TEXT NOT NULL, ")
            sb.append("maxExpansionAllowed INTEGER NOT NULL, ")
            sb.append("externalStorageType INTEGER NOT NULL")
            sb.append(");")
            return sb.toString()
        }
    }
}

