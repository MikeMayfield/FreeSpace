package com.tmf.freespace.datalayer.datasources.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tmf.freespace.BaseApplication
import com.tmf.freespace.datalayer.datasources.local.dao.MediaFileDao
import com.tmf.freespace.datalayer.datasources.local.dao.PropertyBagEntryDao
import com.tmf.freespace.datalayer.models.MediaFile
import com.tmf.freespace.datalayer.models.PropertyBagEntry

/**
 * ROOM database
 */
@Database(
    entities = [
        MediaFile::class,
        PropertyBagEntry::class,
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract val mediaFileDao: MediaFileDao
    abstract val propertyBagEntryDao: PropertyBagEntryDao

    companion object {
        @Volatile // Ensures changes to INSTANCE are immediately visible to other threads
        private var INSTANCE: AppDatabase? = null

        fun instance(): AppDatabase {
            return INSTANCE ?: synchronized(this) { // Synchronize to ensure only one thread initializes
                val instance = Room.databaseBuilder(
                    BaseApplication.instance.applicationContext, // Use application context to avoid memory leaks
                    AppDatabase::class.java,
                    "FreeSpace.db"
                )
                    // .fallbackToDestructiveMigration() // Optional: Handle schema changes by destroying and recreating
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}