package com.tmf.freespace.models

import android.content.Context
import com.tmf.freespace.database.AppDatabase
import java.util.UUID

data class User(
    var id: Int,
    val idGuid: UUID = UUID.randomUUID(),
    val phoneNumber: String,
    val emailAddress: String,
    val password: String,
    var maxExpansionAllowed: Int,
    var cloudStorageType: CloudStorageType,
)
