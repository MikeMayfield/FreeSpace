package com.tmf.freespace.datalayer.repositories

import com.tmf.freespace.datalayer.datasources.local.database.AppDatabase
import com.tmf.freespace.datalayer.models.PropertyBagEntry

class PropertyBagRepository() {
    private val propertyBagEntryDao = AppDatabase.instance().propertyBagEntryDao

    fun allEntries() : List<PropertyBagEntry> {
        return propertyBagEntryDao.getAll()
    }

    suspend fun saveBagEntry(propertyBagEntry: PropertyBagEntry) {
        propertyBagEntryDao.upsert(propertyBagEntry)
    }
}