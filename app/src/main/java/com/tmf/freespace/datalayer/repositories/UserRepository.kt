package com.tmf.freespace.datalayer.repositories

import User
import android.content.Context
import com.tmf.freespace.datalayer.datasources.cloudstorage.CloudStorageType
import com.tmf.freespace.datalayer.datasources.local.database.AppDatabase
import java.util.UUID

class UserRepository(context: Context) {
//    private val userDao = AppDatabase.create(context).userDao

    suspend fun createUser(phoneNumber: String, emailAddress: String, password: String, maxDiskSize: Int, cloudStorageType: CloudStorageType) : User {
        val user = User(UUID.randomUUID(), phoneNumber, emailAddress, password, maxDiskSize, cloudStorageType)
//        userDao.insertIfNew(user)
        return user
    }

    suspend fun getUser() : User? {
//        return userDao.get()
    return null  //TODO
    }
}