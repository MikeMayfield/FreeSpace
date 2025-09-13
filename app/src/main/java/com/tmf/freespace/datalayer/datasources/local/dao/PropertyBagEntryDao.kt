package com.tmf.freespace.datalayer.datasources.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tmf.freespace.datalayer.models.PropertyBagEntry

@Dao
interface PropertyBagEntryDao {
    /**
     * Insert or update a property bag entry
     */
    @Upsert(entity = PropertyBagEntry::class)
    suspend fun upsert(propertyBagEntry: PropertyBagEntry)

    /**
     * Get all property bag entries as a list of PropertyBagEntry objects
     */
    @Query("SELECT * FROM PropertyBag")
    fun getAll(): List<PropertyBagEntry>

    @Query("SELECT * FROM PropertyBag WHERE key = :key")
    fun getByKey(key: String): PropertyBagEntry?
}
