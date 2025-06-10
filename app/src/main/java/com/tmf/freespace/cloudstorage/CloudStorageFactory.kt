package com.tmf.freespace.cloudstorage

import android.content.Context
import com.tmf.freespace.models.CloudStorageType
import com.tmf.freespace.models.User

class CloudStorageFactory {
    fun cloudStorage(user: User, context: Context): ICloudStorage {
        val cloudStorage =  when (user.cloudStorageType) {
            CloudStorageType.Integrated -> InterserverCloudStorage(user, context)
            CloudStorageType.Simulated -> SimulatedCloudStorage(user, context)
        }

        return cloudStorage
    }
}