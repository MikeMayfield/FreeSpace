package com.tmf.freespace.models

import java.util.UUID

data class User(
    val idGuid: UUID = UUID.randomUUID(),
    val phoneNumber: String,
    val emailAddress: String,
    val password: String,
    val maxExpansionAllowed: Int,
    val externalStorageType: ExternalStorageType,
)
