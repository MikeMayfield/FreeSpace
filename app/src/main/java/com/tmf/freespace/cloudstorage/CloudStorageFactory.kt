package com.tmf.freespace.cloudstorage

import com.tmf.freespace.models.CloudStorageType.Simulated
import com.tmf.freespace.models.User

class CloudStorageFactory {
    fun cloudStorage(user: User): ICloudStorage {
        val cloudStorage =  when (user.cloudStorageType) {
//            CloudStorageType.TeraBox -> TeraBoxCloudStorage()
            Simulated -> SimulatedCloudStorage()
            //Add more cases as needed
            else -> SimulatedCloudStorage()
        }
        cloudStorage.init(user)

        return cloudStorage
    }
}