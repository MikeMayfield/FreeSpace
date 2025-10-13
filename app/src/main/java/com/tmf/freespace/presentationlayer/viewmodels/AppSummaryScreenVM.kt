package com.tmf.freespace.presentationlayer.viewmodels

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmf.freespace.BaseApplication
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.repositories.MediaFileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class AppSummaryScreenVM() : ViewModel() {
    private val appContext = BaseApplication.instance.applicationContext
    private val mediaFileRepository = MediaFileRepository(appContext)
    private val propertyBag = PropertyBag(appContext)
    private val _uiState = MutableStateFlow(HomeScreenState())

    val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            periodicallyPopulateHomeScreenState()
        }
    }

    fun updateStatus(value: String) {
        propertyBag.set("STATUS", value)
        _uiState.update { it.copy(status = value) }
    }

    fun updateKeepFreeOptionIdx(value: Int) {
        val currentFreeOptionIdx = propertyBag.getInt("KEEP_FREE_OPTION_IDX", 0)
        if (currentFreeOptionIdx != value) {
            //TODO Compute KeepFree memory value and store in DB; Start background processing if idle; Keep track of status in DB
            val minFreeSpaceGoalMB = minFreeSpaceGoalMB(value)
            propertyBag.setInt("KEEP_FREE_OPTION_IDX", value)
            propertyBag.setLong("MIN_FREE_SPACE_GOAL_MB", minFreeSpaceGoalMB)
            _uiState.update { it.copy(keepFreeOptionIdx = value) }
        }
    }

    fun updateMinFreeSpaceMB(minFreeSpaceMB: Long) {
        propertyBag.setLong("MIN_FREE_SPACE_GOAL_MB", minFreeSpaceMB)
    }

    fun updateSubscriptionStatus(value: HomeScreenState.SubscriptionStatus) {
        //TODO Update status in DB
        _uiState.update { it.copy(subscriptionStatus = value) }
    }

    private suspend fun periodicallyPopulateHomeScreenState() {
        while (true) {
            val uncompressedPhotosAndVideosSize = mediaFileRepository.getTotalUncompressedSize()
            val compressedPhotosAndVideosSize = mediaFileRepository.getTotalCompressedMediaSize()  //Bytes used for compressed media
            val physicalMemorySize = physicalMemorySize()
            val freeSpace = physicalFreeSpaceSize()
            val appsEtcSize = physicalMemorySize - freeSpace - uncompressedPhotosAndVideosSize
            val addedSize = mediaFileRepository.getBytesRecovered()
            _uiState.value = _uiState.value.copy(
                usedMB = (uncompressedPhotosAndVideosSize + appsEtcSize) / 1_000_000L,
                availableNowMB = freeSpace / 1_000_000L,
                currentExpansionMB = addedSize / 1_000_000L,
                expansionAvailableMB = (freeSpace + compressedPhotosAndVideosSize)  / 100_000L,  //100_000 is for nonAppMemory x 10 / 1_000_000
                status = status(),
                keepFreeOptionIdx = propertyBag.getInt("KEEP_FREE_OPTION_IDX", 0),
                subscriptionStatus = HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED,  //TODO  Implement
                physicalMB = physicalMemorySize / 1_000_000L,
            )
            delay(5_000L)  //Update state every n milli-seconds  //TODO Use longer period
        }
    }

    private fun status(): String {
        return "Free expansion limit reached - Subscribe now and never run out of memory again"  //TODO: Generate proper status from DB
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
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.SUBSCRIBED,  //Subscription status
    val physicalMB: Long = 64_000L,  //Amount of physical space available
)
{

    enum class SubscriptionStatus {
        NOT_SUBSCRIBED,
        SUBSCRIBED,
    }

}