package com.tmf.freespace.datalayer.datasources.local

import android.content.Context
import com.tmf.freespace.datalayer.models.PropertyBagEntry
import com.tmf.freespace.datalayer.repositories.PropertyBagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class PropertyBag(val context: Context) {
    var propertyBagRepository = PropertyBagRepository(context)


    /**
     * Returns the value associated with the given key. If the key is not found, returns null.
     */
    fun get(key: String, defaultValue: String = ""): String {
        loadBag()
        return bag[key] ?: defaultValue
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return get(key, defaultValue.toString()).toInt()
    }

    fun getLong(key: String, defaultValue: Long = 0): Long {
        return get(key, defaultValue.toString()).toLong()
    }

    /**
     * Sets the value associated with the given key.
     *
     * @param key The key to associate with the value.
     * @param value The value to set.
     */
    fun set(key: String, value: String) {
        bag[key] = value
        CoroutineScope(Dispatchers.IO).launch {  //Update DB in background I/O thread
            saveBagEntry(PropertyBagEntry(key, value))
        }
    }

    fun setInt(key: String, value: Int) {
        set(key, value.toString())
    }

    fun setLong(key: String, value: Long) {
        set(key, value.toString())
    }


    /**
     * Load property bag from database if it is empty.
     */
    private fun loadBag() {
        if (bag.isEmpty()) {
            runBlocking {
                for (property in propertyBagRepository.allEntries()) {
                    bag[property.key] = property.value
                }

                if (bag.isEmpty()) {
                    bag["__~EMPTY__"] = ""
                }
            }
        }
    }

    /**
     * Save new/changed property bag entry to database
     */
    private suspend fun saveBagEntry(bagEntry: PropertyBagEntry) {
        propertyBagRepository.saveBagEntry(bagEntry)
    }


    companion object {
        private val bag: ConcurrentHashMap<String, String> = ConcurrentHashMap()  //Thread-safe property bag in memory
    }
}