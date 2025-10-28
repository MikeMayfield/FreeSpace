package com.tmf.freespace.datalayer.repositories

import android.content.Context
import com.tmf.freespace.datalayer.datasources.local.database.AppDatabase
import com.tmf.freespace.datalayer.models.PropertyBagEntry

class PropertyBagRepository(val context: Context) {
    private val propertyBagEntryDao = AppDatabase.instance(context).propertyBagEntryDao

    suspend fun allEntries() : List<PropertyBagEntry> {
        return propertyBagEntryDao.getAll()
    }

    suspend fun saveBagEntry(propertyBagEntry: PropertyBagEntry) {
        propertyBagEntryDao.upsert(propertyBagEntry)
    }


    /**
     * Returns the value associated with the given key. If the key is not found, returns null.
     */
    suspend fun get(key: String, defaultValue: String = ""): String {
        loadBag()
        return bag[key] ?: defaultValue
    }

    /**
     * Sets the value associated with the given key.
     *
     * @param key The key to associate with the value.
     * @param value The value to set.
     */
    suspend fun set(key: String, value: String) {
        loadBag()
        bag[key] = value
        propertyBagEntryDao.upsert(PropertyBagEntry(key, value))
    }


    /**
     * Load property bag from database if it is empty.
     */
    private suspend fun loadBag() {
        if (bag.isEmpty()) {
            for (property in propertyBagEntryDao.getAll()) {
                bag[property.key] = property.value
            }
        }
    }


    companion object {
        private val bag = mutableMapOf<String, String>()
    }
}