package com.tmf.freespace.presentationlayer.viewmodels

import android.app.Activity
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import android.os.storage.StorageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.tmf.freespace.BaseApplication
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.IS_IDLE
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.KEEP_FREE_OPTION_IDX
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.SUBSCRIPTION_STATUS
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.TRIAL_GB_FREE
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import com.tmf.freespace.domainlayer.general.DLog
import com.tmf.freespace.presentationlayer.viewmodels.HomeScreenState.SubscriptionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.min


class CommonViewModel() : ViewModel() {
    val tag = "CommonViewModel"
    val bytesToMB = 1_000_000L

    private val appContext = BaseApplication.instance.applicationContext
    private val mediaFileRepository = MediaFileRepository()

    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()

    val products: StateFlow<Map<String, ProductDetails>> = BaseApplication.billingClient!!.productDetails

    init {
        viewModelScope.launch(Dispatchers.IO) {
            periodicallyPopulateHomeScreenState()
        }
    }


    fun updateKeepFreeOptionIdx(value: Int) {
        PropertyBag.setInt(KEEP_FREE_OPTION_IDX, value)
        _uiState.update { it.copy(
            keepFreeOptionIdx = value
        ) }
    }

    /**
     * Launches the billing flow for a given product ID.
     *
     * @param activity The activity to launch the billing flow from.
     * @param productId The ID of the product to purchase.
     */
    fun launchPurchaseFlow(activity: Activity, productId: String): Boolean {
        return BaseApplication.billingClient?.launchPurchaseFlow(activity, productId) ?: false
    }

    var isSubscribed: Boolean
        get() = PropertyBag.getString(SUBSCRIPTION_STATUS) != SubscriptionStatus.NOT_SUBSCRIBED.name
        set(value) {
            PropertyBag.setString(SUBSCRIPTION_STATUS, if (value) SubscriptionStatus.SUBSCRIBED.name else SubscriptionStatus.NOT_SUBSCRIBED.name)
            viewModelScope.launch {
                populateHomeScreenState()
            }
        }


    //region Private methods

    private suspend fun periodicallyPopulateHomeScreenState() {
        while (true) {
            populateHomeScreenState()
            delay(5_000L)  //Poll state every n milli-seconds
        }
    }

    suspend fun populateHomeScreenState() {
        BaseApplication.billingClient?.querySubscriptionStatus() {
            viewModelScope.launch {
                populateHomeScreenState()  //Force isSubscribed state to update ASAP
            }
        }

        val physicalMemorySize = physicalMemorySize()
        val uncompressedPhotosAndVideosSize = mediaFileRepository.getTotalUncompressedMediaSize()
        val compressedPhotosAndVideosSize = mediaFileRepository.getTotalCompressedMediaSize()
        val bytesRecovered = uncompressedPhotosAndVideosSize - compressedPhotosAndVideosSize
        val freeMemory = freeMemorySize()  //6GB
        val appsEtcSize = min((physicalMemorySize / 5L), (physicalMemorySize - freeMemory))  //No API to return total app usage. Use estimate based on physical memory size on the assumption that bigger phones end up with more apps. 20% of physical memory, but no more than actual physical usage
        _uiState.value = _uiState.value.copy(
            uncompressedMB = (uncompressedPhotosAndVideosSize + appsEtcSize) / bytesToMB,
            freeMemoryMB = freeMemory / bytesToMB,
            currentExpansionMB = bytesRecovered / bytesToMB,
            expansionAvailableMB = expansionAvailableMB(freeMemory, uncompressedPhotosAndVideosSize, compressedPhotosAndVideosSize),
            addedMB = (bytesRecovered - compressedPhotosAndVideosSize),
            status = status(),
            keepFreeOptionIdx = PropertyBag.getInt(KEEP_FREE_OPTION_IDX),
            physicalMB = physicalMemorySize / bytesToMB,
            isSubscribed = isSubscribed
        )
    }

    private fun expansionAvailableMB(freeMemory: Long, uncompressedPhotosAndVideosSize: Long, compressedPhotosAndVideosSize: Long): Long {
        val freeMemoryExpansion = freeMemory * 10L  //Expansion of free memory is estimated at at least 10x

        //Existing media may be able to be compressed more than it already is, up to at least 10x, based on how much it has already been compressed. Most media typically hasn't been compressed at all
        val compressionRatio = uncompressedPhotosAndVideosSize / (compressedPhotosAndVideosSize + 1f)  //NOTE: Add 1 to divisor to ensure no divide-by-zero
        val remainingCompressionRatio = min(11f - compressionRatio, 11f)  //Change 1..11 current ratio to 11..1 remaining ratio (approximate)
        val additionalExpansion = (compressedPhotosAndVideosSize * remainingCompressionRatio).toLong()

        return (freeMemoryExpansion + additionalExpansion) / bytesToMB
    }

    private suspend fun status(): String {
        val addedSize = mediaFileRepository.getBytesRecovered()
        if (addedSize >= PropertyBag.getInt(TRIAL_GB_FREE) * 1_000_000_000L && !isSubscribed) {
            return "TRIAL LIMIT — Free expansion limit reached. Subscribe now and get almost unlimited memory"
        }

        val isIdle = PropertyBag.getBoolean(IS_IDLE)
        if (isIdle) {
            if (batteryLow()) {
                return "LOW BATTERY — Waiting for your battery to charge. Plug in to expand memory now"
            }
            else {
                return "IDLE — Extra memory will be automatically added when needed"
            }
        }
        else {
            return "ADDING MEMORY — Please check back later. Magic takes time..."
        }
    }

    /**
     * Returns the total amount of storage on the primary disk, adjusted to standardized memory sizes (e.g. 128GB, 256GB, etc.)
     *
     * @return Total amount of storage on the primary disk, in bytes
     */
    fun physicalMemorySize(): Long {
        if (savedPhysicalMemorySize == 0L) {
            val storageManager = appContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val statsManager = appContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

            var actualPhysicalMemorySize = 0L

            for (volume in storageManager.storageVolumes) {
                try {
                    if (volume.isPrimary) {
                        val uuid = volume.uuid?.let { UUID.fromString(it) } ?: StorageManager.UUID_DEFAULT
                        actualPhysicalMemorySize = statsManager.getTotalBytes(uuid)
                        break
                    }
                }
                catch (e: Exception) {
                    DLog.e(tag, "Error in physicalMemorySize: ${e.message}")
                }
            }

            //Standardize memory sizes to 1GB, 2GB, 4GB, 8GB, 16GB, etc.)
            savedPhysicalMemorySize = when {
                actualPhysicalMemorySize % 1_000_000_000L == 0L -> actualPhysicalMemorySize  //Use actual size if it's a multiple of 1GB
                actualPhysicalMemorySize < 10_000_000_000L -> actualPhysicalMemorySize
                actualPhysicalMemorySize < 18_000_000_000L -> 16_000_000_000L
                actualPhysicalMemorySize < 35_000_000_000L -> 32_000_000_000L
                actualPhysicalMemorySize < 65_000_000_000L -> 64_000_000_000L
                actualPhysicalMemorySize < 130_000_000_000L -> 128_000_000_000L
                actualPhysicalMemorySize < 260_000_000_000L -> 256_000_000_000L
                actualPhysicalMemorySize < 520_000_000_000L -> 512_000_000_000L
                actualPhysicalMemorySize < 1_048_576_000_000L -> 1_024_000_000_000L
                else -> 2_048_000_000L
            }
        }

        return savedPhysicalMemorySize
    }
    var savedPhysicalMemorySize = 0L

    /*
     * Returns the amount of free space on the primary disk
     */
    fun freeMemorySize(): Long {
        val storageManager = appContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val statsManager = appContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

        for (volume in storageManager.storageVolumes) {
            try {
                if (volume.isPrimary) {
                    val uuid = volume.uuid?.let { UUID.fromString(it) } ?: StorageManager.UUID_DEFAULT
                    return statsManager.getFreeBytes(uuid)
                }
            }
            catch (e: Exception) {
                DLog.e(tag, "Error in freeMemorySize: ${e.message}")
            }
        }

        return 0L
    }

    private fun batteryLow(): Boolean {
        val batteryManager = appContext.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batterLevelPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return batterLevelPct <= 33
    }

    //endregion
}


data class HomeScreenState(
    val uncompressedMB: Long = 0L,  //Amount of space used for photos, videos, etc. (MB)
    val freeMemoryMB: Long = 0L,  //Amount of space available now (MB)
    val currentExpansionMB: Long = 0L,  //Amount of space already added by FreeSpace (MB)
    val expansionAvailableMB: Long = 320_000L,  //Maximum amount of space available for expansion (MB)
    val addedMB: Long = 0L,  //Amount of space added through optimization/compression (MB)
    val status: String = "",  //Status, as display string
    val keepFreeOptionIdx: Int = 1,  //Index of keep free drop-down option selected by user
    val physicalMB: Long = 64_000L,  //Amount of physical space on device (MB)
    val isSubscribed: Boolean = false,
)
{

    enum class SubscriptionStatus {
        NOT_SUBSCRIBED,
        SUBSCRIBED,
    }

}