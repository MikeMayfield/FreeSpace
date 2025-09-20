package com.tmf.freespace.datalayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PropertyBag entry for bags of key/value pairs
 *
 * @property key The unique ID of the property bag element
 * @property value The value of the property bag element
 */
@Entity(tableName = "PropertyBag")
data class PropertyBagEntry(
    @PrimaryKey val key: String,
    val value: String
) {

    companion object {
        const val DESIRED_FREE_SPACE_GB = "DesiredFreeSpaceGB"
        const val SHOULD_COMPRESS_IMAGES = "ShouldCompressImages"
        const val SHOULD_COMPRESS_VIDEOS = "ShouldCompressVideos"
        const val SHOULD_COMPRESS_AUDIO = "ShouldCompressAudio"
        const val SHOULD_COMPRESS_DOCUMENTS = "ShouldCompressDocuments"
        const val SHOULD_COMPRESS_OTHER_FILES = "ShouldCompressOtherFiles"
        const val REQUIRE_WIFI = "RequireWifi"
        const val SCREEN_MUST_BE_OFF = "ScreenMustBeOff"
        const val MEDIA_STORE_VERSION = "MediaStoreVersion"  //Version of MediaStore when database was last updated
        const val MAX_DATE_ADDED = "MaxDateAdded"  //Earliest date to search in MediaStore
    }
}