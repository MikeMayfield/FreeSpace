package com.tmf.freespace.datalayer.datasources.local

import android.app.Application
import com.tmf.freespace.datalayer.models.PropertyBagEntry
import com.tmf.freespace.datalayer.repositories.PropertyBagRepository

class PropertyBag(val application: Application) {
    var propertyBagRepository = PropertyBagRepository(application)

    /**
     * Returns the value associated with the given key. If the key is not found, returns null.
     */
    suspend fun get(key: String): String? {
        loadBag()
        return bag[key]
    }

    /**
     * Sets the value associated with the given key.
     *
     * @param key The key to associate with the value.
     * @param value The value to set.
     */
    suspend fun set(key: String, value: String) {
        bag[key] = value
        saveBagEntry(PropertyBagEntry(key, value))
    }


    /**
     * Load property bag from database if it is empty.
     */
    private suspend fun loadBag() {
        if (bag.isEmpty()) {
            for (property in propertyBagRepository.allEntries()) {
                bag[property.key] = property.value
            }
        }
    }

    /**
     *
     */
    private suspend fun saveBagEntry(bagEntry: PropertyBagEntry) {
        propertyBagRepository.saveBagEntry(bagEntry)
    }


    companion object {
        private val bag = mutableMapOf<String, String>()
    }
}