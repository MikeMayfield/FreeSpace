package com.tmf.freespace.models

data class Preferences(
    var desiredFreeSpaceGB: Int = 10,
    var shouldCompressImages: Boolean = true,
    var shouldCompressVideos: Boolean = true,
    var shouldCompressAudios: Boolean = true,
    var shouldCompressDocuments: Boolean = true,
    var shouldCompressOtherFiles: Boolean = true,
    var backupAllFiles: Boolean = false,
    var requireWifi: Boolean = true,
    var screenMustBeOff: Boolean = true,
    var emailAddress: String = "",
    var password: String = "",
)
