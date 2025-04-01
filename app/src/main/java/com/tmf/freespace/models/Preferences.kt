package com.tmf.freespace.models

data class Preferences(
    val desiredFreeSpaceGB: Int = 10,
    val shouldCompressImages: Boolean = true,
    val shouldCompressVideos: Boolean = true,
    val shouldCompressAudios: Boolean = true,
    val shouldCompressDocuments: Boolean = true,
    val shouldCompressOtherFiles: Boolean = true,
    val requireWifi: Boolean = true,
    val screenMustBeOff: Boolean = true,
    val emailAddress: String = "",
    val password: String = "",
)
