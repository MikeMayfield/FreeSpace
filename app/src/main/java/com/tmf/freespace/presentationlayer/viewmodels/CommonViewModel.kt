package com.tmf.freespace.presentationlayer.viewmodels

import android.app.Activity
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
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
import com.tmf.freespace.presentationlayer.viewmodels.HomeScreenState.SubscriptionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class CommonViewModel() : ViewModel() {
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
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        BaseApplication.billingClient?.launchPurchaseFlow(activity, productId)
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
        val uncompressedPhotosAndVideosSize = mediaFileRepository.getTotalUncompressedMediaSize()
        val compressedPhotosAndVideosSize = mediaFileRepository.getTotalCompressedMediaSize()
        val physicalMemorySize = physicalMemorySize()
        val freeSpace = physicalFreeSpaceSize()
        val appsEtcSize = physicalMemorySize - freeSpace - uncompressedPhotosAndVideosSize
        val addedSize = mediaFileRepository.getBytesRecovered()
        val expansionAvailableFromCompression = (uncompressedPhotosAndVideosSize * 10L) - (uncompressedPhotosAndVideosSize - compressedPhotosAndVideosSize)  //10:1 media compression, minus amount already recovered
        val expansionAvailableFromCompressingFreeSpace = freeSpace * 10L  //Assuming 10:1 total expansion from optimizing future media, as added
        _uiState.value = _uiState.value.copy(
            usedMB = (uncompressedPhotosAndVideosSize + appsEtcSize) / bytesToMB,
            availableNowMB = freeSpace / bytesToMB,
            currentExpansionMB = addedSize / bytesToMB,
            expansionAvailableMB = (expansionAvailableFromCompression + expansionAvailableFromCompressingFreeSpace)  / bytesToMB,
            status = status(),
            keepFreeOptionIdx = PropertyBag.getInt(KEEP_FREE_OPTION_IDX),
            physicalMB = physicalMemorySize / bytesToMB,
            isSubscribed = isSubscribed
        )
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

    private fun physicalMemorySize(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val totalBlocks = stat.blockCountLong
        val blockSize = stat.blockSizeLong

        return  totalBlocks * blockSize
    }

    private fun physicalFreeSpaceSize(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val availableBlocks = stat.availableBlocksLong
        val blockSize = stat.blockSizeLong

        return availableBlocks * blockSize
    }

    private fun batteryLow(): Boolean {
        val batteryManager = appContext.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batterLevelPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return batterLevelPct <= 33
    }

    //endregion
}


data class HomeScreenState(
    val usedMB: Long = 0L,  //Amount of space used for photos, videos, etc. (Megabytes)
    val availableNowMB: Long = 0L,  //Amount of space available now
    val currentExpansionMB: Long = 0L,  //Amount of space already added by FreeSpace
    val expansionAvailableMB: Long = 320_000L,  //Maximum amount of space available for expansion
    val addedMB: Long = 0L,  //Amount of space added through optimization/compression
    val status: String = "",  //Status, as display string
    val keepFreeOptionIdx: Int = 1,  //Index of keep free option selected by user
    val physicalMB: Long = 64_000L,  //Amount of physical space available
    val isSubscribed: Boolean = false,
)
{

    enum class SubscriptionStatus {
        NOT_SUBSCRIBED,
        SUBSCRIBED,
    }

}