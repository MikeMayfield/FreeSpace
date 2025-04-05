package com.tmf.freespace.cloudstorage

import com.tmf.freespace.models.CloudStorageType
import com.tmf.freespace.models.CloudStorageType.Simulated

class CloudStorageFactory {
    fun CloudStorage(cloudStorageType: CloudStorageType): ICloudStorage {
        return when (cloudStorageType) {
//            CloudStorageType.TeraBoxFree -> TeraBoxFreeCloudStorage()
            else -> SimulatedCloudStorage()
        }
    }
}