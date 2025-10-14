package com.tmf.freespace.datalayer.datasources.local

import android.content.Context
import com.tmf.freespace.datalayer.models.PropertyBagEntry
import com.tmf.freespace.datalayer.repositories.PropertyBagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return get(key, defaultValue.toString()).toBoolean()
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

    fun setBoolean(key: String, value: Boolean) {
        set(key, value.toString())
    }


    /**
     * Load property bag from database if it is empty.
     */
    private fun loadBag() {
        if (bag.isEmpty()) {
            runBlocking {  //Stall main thread until bag is loaded the first time. This is not ideal, but works for now since the property bag is only loaded once.
                val allProperties = CoroutineScope(Dispatchers.IO).async {
                    propertyBagRepository.allEntries()
                }.await()
                for (property in allProperties) {
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
        CoroutineScope(Dispatchers.IO).launch {
            propertyBagRepository.saveBagEntry(bagEntry)
        }
    }


    companion object {
        private val bag: ConcurrentHashMap<String, String> = ConcurrentHashMap()  //Thread-safe property bag in memory, shared by all users of PropertyBag class

        //Known properties (KEYS)
        const val MAX_DATE_ADDED = "MAX_DATE_ADDED"
        const val KEEP_FREE_OPTION_IDX = "KEEP_FREE_OPTION_IDX"
        const val MIN_FREE_SPACE_GOAL_MB = "MIN_FREE_SPACE_GOAL_MB"
        const val IS_IDLE = "IS_IDLE"
        const val PRIOR_MEDIA_STORE_VERSION = "PRIOR_MEDIA_STORE_VERSION"
        const val SUBSCRIPTION_STATUS = "SUBSCRIPTION_STATUS"
        const val TRIAL_GB_FREE = "TRIAL_GB_FREE"
        const val MAX_GB_LIMIT_FOR_PLAN = "MAX_GB_LIMIT_FOR_PLAN"
        const val MIN_GB_FREE_GOAL = "MIN_GB_FREE_GOAL"
        const val ALWAYS_OPTIMIZE_LEVEL = "ALWAYS_OPTIMIZE_LEVEL"
    }
}