package com.tmf.freespace.datalayer.repositories

import User
import android.content.Context
import com.tmf.freespace.datalayer.datasources.cloudstorage.CloudStorageType
import com.tmf.freespace.datalayer.datasources.network.ServerIO
import java.util.UUID

class UserRepository(val context: Context) {
//    private val userDao = AppDatabase.create(context).userDao
    private val serverIO = ServerIO()

//TODO    fun createUser(phoneNumber: String, emailAddress: String, password: String, maxDiskSize: Int, cloudStorageType: CloudStorageType) : User {
//        val user = User(UUID.randomUUID(), phoneNumber, emailAddress, password, maxDiskSize, cloudStorageType)
////        userDao.insertIfNew(user)
//        return user
//    }

    fun getUser() : User {
//        return AppDatabase.create(context).userDao.get()
        return User(UUID.fromString("dfc3f61f-e302-4b79-874c-fef5b1e1d78b"), "", "", "", 0, CloudStorageType.Integrated)  //TODO Get from DB
    }

    /**
     * Send heartbeat message to server
     */
    fun sendHeartbeat() {
        val user = getUser()
        serverIO.sendHeartbeat(user.idGuid.toString())
    }
}