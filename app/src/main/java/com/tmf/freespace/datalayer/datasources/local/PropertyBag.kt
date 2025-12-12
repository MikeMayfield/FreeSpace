package com.tmf.freespace.datalayer.datasources.local

import com.tmf.freespace.datalayer.models.PropertyBagEntry
import com.tmf.freespace.datalayer.repositories.PropertyBagRepository
import com.tmf.freespace.presentationlayer.viewmodels.HomeScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object PropertyBag {
    private var propertyBagRepository = PropertyBagRepository()
    private val bag: ConcurrentHashMap<String, String> = ConcurrentHashMap()  //Thread-safe property bag in memory, shared by all users of PropertyBag class
    private val diskWriteQueue = ConcurrentLinkedQueue<PropertyBagEntry>()
    private var writeJob: Job? = null
    private val writeMutex = Mutex()

    init {
        loadBag(propertyBagRepository)
    }

    /**
     * Returns the value associated with the given key. If the key is not found, returns null.
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return bag.getOrDefault(key, defaultValue)
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return getString(key, defaultValue.toString()).toInt()
    }

    fun getLong(key: String, defaultValue: Long = 0): Long {
        return getString(key, defaultValue.toString()).toLong()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return getString(key, defaultValue.toString()).toBoolean()
    }

    /**
     * Sets the string value associated with the given key.
     *
     * @param key The key to associate with the value.
     * @param value The value to set.
     */
    fun setString(key: String, value: String) {
        if (!bag.containsKey(key) || bag[key] != value) {
            bag.put(key, value)
            queueForDiskWrite(PropertyBagEntry(key, value))
        }
    }

    fun setInt(key: String, value: Int) {
        setString(key, value.toString())
    }

    fun setLong(key: String, value: Long) {
        setString(key, value.toString())
    }

    fun setBoolean(key: String, value: Boolean) {
        setString(key, value.toString())
    }

    /**
     * Load property bag from database if it is empty.
     */
    private fun loadBag(propertyBagRepository: PropertyBagRepository) {
        runBlocking {  //Stall main thread until bag is loaded the first time. This is not ideal, but works for now since the property bag is only loaded once.
            val allProperties = CoroutineScope(Dispatchers.IO).async {
                propertyBagRepository.allEntries()
            }.await()
            if (allProperties.isEmpty()) {
                initPropertyBag()
            }
            else {
                for (property in allProperties) {
                    bag.put(property.key, property.value)
                }
            }
        }
    }

    /**
     * Initialize the property bag with default values the first time it is used
     */
    private fun initPropertyBag() {
        setString(MAX_DATE_ADDED, "0")
        setString(KEEP_FREE_OPTION_IDX, "1")
        setString(MIN_FREE_SPACE_GOAL_MB, "5000")
        setString(IS_IDLE, "true")
        setString(PRIOR_MEDIA_STORE_VERSION, "0")
        setString(SUBSCRIPTION_STATUS, HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED.toString())
        setString(TRIAL_GB_FREE, "10")
    }

    /**
     * Adds an entry to a thread-safe queue and launches a single coroutine to process the entire queue.
     * This prevents launching excessive coroutines for rapid updates.
     *
     * @param bagEntry The [PropertyBagEntry] to be saved to disk.
     */
    private fun queueForDiskWrite(bagEntry: PropertyBagEntry) {
        diskWriteQueue.add(bagEntry)

        // Use a mutex to ensure only one writer coroutine is active at a time.
        CoroutineScope(Dispatchers.IO).launch {
            writeMutex.withLock {
                // If a write job is already active, it will handle the newly queued item.
                if (writeJob == null || writeJob?.isCompleted == true) {
                    writeJob = launch {
                        // Process all items currently in the queue.
                        while (diskWriteQueue.isNotEmpty()) {
                            val entryToWrite = diskWriteQueue.poll()
                            entryToWrite?.let {
                                propertyBagRepository.saveBagEntry(it)
                            }
                        }
                    }
                }
            }
        }
    }




    //Known properties (KEYS)
    const val MAX_DATE_ADDED = "MAX_DATE_ADDED"  //Max creation date found in any media file added to database
    const val KEEP_FREE_OPTION_IDX = "KEEP_FREE_OPTION_IDX"  //Current drop-down index for Keep Free option on AppSummaryScreen
    const val MIN_FREE_SPACE_GOAL_MB = "MIN_FREE_SPACE_GOAL_MB"  //Min MB to try to always leave free. Attempt to create more memory if below this goal
    const val IS_IDLE = "IS_IDLE"  //Flag: The background service is idle (i.e. not running)
    const val PRIOR_MEDIA_STORE_VERSION = "PRIOR_MEDIA_STORE_VERSION"  //Latest media store version seen. If newer version becomes available, database needs to be updated with possibly different MediaIDs
    const val SUBSCRIPTION_STATUS = "SUBSCRIPTION_STATUS"  //Current subscription status
    const val TRIAL_GB_FREE = "TRIAL_GB_FREE"  //Size of free trial in GB
}