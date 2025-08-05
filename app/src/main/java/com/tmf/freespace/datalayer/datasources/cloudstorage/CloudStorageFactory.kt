package com.tmf.freespace.datalayer.datasources.cloudstorage

import android.content.Context
import com.tmf.freespace.datalayer.datasources.database.AppDatabase
import com.tmf.freespace.models.CloudStorageType
import com.tmf.freespace.models.User

class CloudStorageFactory {
    fun cloudStorage(user: User, context: Context, database: AppDatabase): ICloudStorage {
        val cloudStorage =  when (user.cloudStorageType) {
            CloudStorageType.Integrated -> InterserverCloudStorage(user, database)
            CloudStorageType.Simulated -> SimulatedCloudStorage(user, context)
        }

        return cloudStorage
    }
}