package com.tmf.freespace.cloudstorage

import android.content.Context
import com.tmf.freespace.models.CloudStorageType
import com.tmf.freespace.models.User

class CloudStorageFactory {
    fun cloudStorage(user: User, context: Context): ICloudStorage {
        val cloudStorage =  when (user.cloudStorageType) {
            CloudStorageType.TeraBox -> SimulatedCloudStorage()  //TODO Implement real cloud storage
            CloudStorageType.GoggleDrive -> SimulatedCloudStorage()  //TODO Implement real cloud storage
            CloudStorageType.DropBox -> SimulatedCloudStorage()  //TODO Implement real cloud storage
            CloudStorageType.Simulated -> SimulatedCloudStorage()
        }
        cloudStorage.init(user, context)

        return cloudStorage
    }
}