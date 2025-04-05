package com.tmf.freespace.services

import android.content.Context
import com.tmf.freespace.database.AppDatabase
import kotlinx.coroutines.runBlocking

class CompressionService {
    lateinit var database: AppDatabase

    //MUST be called on a background thread or launch one or main thread will block
    fun start(context: Context) {  //TODO Implement this properly for running in a background thread on the service
        runBlocking {
            database = AppDatabase(context.applicationContext)

            val compressFiles = CompressionServiceHelper(context.applicationContext, database)
            compressFiles.start()
        }
    }
}
