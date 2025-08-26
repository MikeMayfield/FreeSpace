package com.tmf.freespace.datalayer.datasources.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tmf.freespace.datalayer.datasources.local.dao.MediaFileDao
import com.tmf.freespace.datalayer.datasources.local.dao.UserDao
import com.tmf.freespace.datalayer.models.MediaFile

/**
 * ROOM database
 */
@Database(
    entities = [
        MediaFile::class,
//        User::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract val mediaFileDao: MediaFileDao
//    abstract val userDao: UserDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java, "FreeSpace.db"
            ).build()
        }
    }
}