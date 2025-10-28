package com.tmf.freespace.datalayer.repositories

import android.content.Context
import com.tmf.freespace.datalayer.datasources.local.database.AppDatabase
import com.tmf.freespace.datalayer.models.PropertyBagEntry

class PropertyBagRepository(val context: Context) {
    private val propertyBagEntryDao = AppDatabase.instance(context).propertyBagEntryDao

    fun allEntries() : List<PropertyBagEntry> {
        return propertyBagEntryDao.getAll()
    }

    suspend fun saveBagEntry(propertyBagEntry: PropertyBagEntry) {
        propertyBagEntryDao.upsert(propertyBagEntry)
    }
}