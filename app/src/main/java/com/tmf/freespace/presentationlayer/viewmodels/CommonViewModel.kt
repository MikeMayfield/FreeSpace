package com.tmf.freespace.presentationlayer.viewmodels

import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmf.freespace.BaseApplication
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.IS_IDLE
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.KEEP_FREE_OPTION_IDX
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.MIN_FREE_SPACE_GOAL_MB
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.SUBSCRIPTION_STATUS
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class CommonViewModel() : ViewModel() {
    val bytesToMB = 1_000_000L

    private val appContext = BaseApplication.instance.applicationContext
    private val mediaFileRepository = MediaFileRepository(appContext)
    private val _uiState = MutableStateFlow(HomeScreenState())

    val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            periodicallyPopulateHomeScreenState()  //TODO Only do if permissions have been obtained. Maybe it would be better to call it on demand
        }
    }


    fun updateKeepFreeOptionIdx(value: Int) {
        //TODO Compute KeepFree memory value and store in DB; Start background processing if idle; Keep track of status in DB
        val minFreeSpaceGoalMB = minFreeSpaceGoalMB(value)
        PropertyBag.setInt(KEEP_FREE_OPTION_IDX, value)
        PropertyBag.setLong(MIN_FREE_SPACE_GOAL_MB, minFreeSpaceGoalMB)
        _uiState.update { it.copy(
            keepFreeOptionIdx = value
        ) }
    }

//    fun updateSubscriptionStatus(value: HomeScreenState.SubscriptionStatus) {
//        propertyBag.set(SUBSCRIPTION_STATUS, value.name)
//        _uiState.update { it.copy(subscriptionStatus = value) }
//    }

    private suspend fun periodicallyPopulateHomeScreenState() {
        while (true) {
            val uncompressedPhotosAndVideosSize = mediaFileRepository.getTotalUncompressedSize()
            val compressedPhotosAndVideosSize = mediaFileRepository.getTotalCompressedMediaSize()
            val physicalMemorySize = physicalMemorySize()
            val freeSpace = physicalFreeSpaceSize()
            val appsEtcSize = physicalMemorySize - freeSpace - uncompressedPhotosAndVideosSize
            val addedSize = mediaFileRepository.getBytesRecovered()
            val expansionAvailableFromCompression = (uncompressedPhotosAndVideosSize - compressedPhotosAndVideosSize) * 8L
            val expansionAvailableFromFreeSpace = freeSpace * 10L
            _uiState.value = _uiState.value.copy(
                usedMB = (uncompressedPhotosAndVideosSize + appsEtcSize) / bytesToMB,
                availableNowMB = freeSpace / bytesToMB,
                currentExpansionMB = addedSize / bytesToMB,
                expansionAvailableMB = (expansionAvailableFromCompression + expansionAvailableFromFreeSpace)  / bytesToMB,
                status = status(),
                keepFreeOptionIdx = PropertyBag.getInt(KEEP_FREE_OPTION_IDX, 0),
                subscriptionStatus = HomeScreenState.SubscriptionStatus.valueOf(PropertyBag.get(SUBSCRIPTION_STATUS, HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED.toString())),  //TODO  Implement
                physicalMB = physicalMemorySize / bytesToMB,
            )
            delay(1_000L)//TODO  //Poll state every n milli-seconds
        }
    }

    private suspend fun status(): String {
        val addedSize = mediaFileRepository.getBytesRecovered()
        if (addedSize >= 8_000_000_000L && !isSubscribed()) {
            return "TRIAL LIMIT — Free expansion limit reached. Subscribe now and get almost unlimited memory"
        }

        val isIdle = PropertyBag.getBoolean(IS_IDLE, true)
        if (isIdle) {
            if (batteryLow()) {
                return "LOW BATTERY — Waiting for your battery to charge. Plug in to expand memory now"
            }
            else {
                return "IDLE — Extra memory will be automatically added when needed"
            }
        }
        else {
            return "ADDING MEMORY — Adding memory; please check back later. Magic takes time..."
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

    private fun isSubscribed(): Boolean {
        return PropertyBag.get(SUBSCRIPTION_STATUS, HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED.name) != HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED.name
    }


    private fun minFreeSpaceGoalMB(keepFreeOptionIdx: Int): Long {
        return when (keepFreeOptionIdx) {
            0 -> 2000L  //2GB
            1 -> 5000L  //5GB
            2 -> 10000L  //10GB
            3 -> (physicalMemorySize() * 0.05f).toLong()  //5%
            4 -> (physicalMemorySize() * 0.10f).toLong()  //10%
            else -> 0
        }
    }
}

data class HomeScreenState(
    val usedMB: Long = 0L,  //Amount of space used for photos, videos, etc. (Megabytes)
    val availableNowMB: Long = 0L,  //Amount of space available now
    val currentExpansionMB: Long = 0L,  //Amount of space already added by FreeSpace
    val expansionAvailableMB: Long = 320_000L,  //Maximum amount of space available for expansion (8GB Lite, 10 x Physical for MAX)
    val addedMB: Long = 0L,  //Amount of space added through optimization/compression
    val status: String = "",  //Status, as display string
    val keepFreeOptionIdx: Int = 0,  //Index of keep free option selected by user
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.NOT_SUBSCRIBED,  //Subscription status
    val physicalMB: Long = 64_000L,  //Amount of physical space available
)
{

    enum class SubscriptionStatus {
        NOT_SUBSCRIBED,
        SUBSCRIBED,
    }

}