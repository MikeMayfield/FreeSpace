package com.tmf.freespace.datalayer.datasources.local.database

import User
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tmf.freespace.datalayer.datasources.local.dao.MediaFileDao
import com.tmf.freespace.datalayer.datasources.local.dao.PropertyBagEntryDao
import com.tmf.freespace.datalayer.datasources.local.dao.UserDao
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.models.PropertyBagEntry

/**
 * ROOM database
 */
@Database(
    entities = [
        MediaFile::class,
        User::class,
        PropertyBagEntry::class,
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract val mediaFileDao: MediaFileDao
    abstract val userDao: UserDao
    abstract val propertyBagEntryDao: PropertyBagEntryDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java, "FreeSpace.db"
            ).build()
        }
    }
}