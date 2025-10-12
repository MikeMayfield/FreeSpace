package com.tmf.freespace.presentationlayer.viewmodels

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmf.freespace.BaseApplication
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.models.MediaType
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

    private fun isSubscribed(): Boolean {
        return _uiState.value.subscriptionStatus == HomeScreenState.SubscriptionStatus.SUBSCRIBED
    }

    private suspend fun periodicallyPopulateHomeScreenState() {
        while (true) {
            val photosSize = mediaFileRepository.getTotalSizeByMediaType(MediaType.IMAGE)
            val videosSize = mediaFileRepository.getTotalSizeByMediaType(MediaType.VIDEO)
            val physicalMemorySize = physicalMemorySize()
            val freeSpace = physicalFreeSpaceSize()
            val appsSize = (physicalMemorySize - freeSpace - photosSize - videosSize)
            _uiState.value = _uiState.value.copy(
                photosMB = photosSize / 1_000_000L,
                videosMB = videosSize / 1_000_000L,
                appsMB = appsSize / 1_000_000L,
                addedMB = mediaFileRepository.getBytesRecovered() / 1_000_000L,
                maxExpansionMB = (physicalMemorySize - appsSize) / 100_000L,  //100_000 is for non-app memory x 10 / 1_000_000
                minFreeSpaceGoalMB = minFreeSpaceGoalMB(propertyBag.getInt("KEEP_FREE_OPTION_IDX", 0)),
                status = status(),
                keepFreeOptionIdx = propertyBag.getInt("KEEP_FREE_OPTION_IDX", 0),
                subscriptionStatus = HomeScreenState.SubscriptionStatus.NOT_SUBSCRIBED,  //TODO  Implement
            )
            delay(5_000L)  //Update state every n milli-seconds  //TODO Use longer period
        }
    }

    private fun status(): String {
        return "Limit reached - Upgrade to Max for more"  //TODO: Generate proper status from DB
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
    val photosMB: Long = 0L,  //Amount of space used for photos (Megabytes)
    val videosMB: Long = 0L,  //Amount of space used for videos (Megabytes)
    val appsMB: Long = 0L,  //Amount of space used for apps, computed as physicalSpace - freeSpace - photosMB - videosMB
    val addedMB: Long = 0L,  //Amount of space added through optimization/compression
    val maxExpansionMB: Long = 320_000L,  //Maximum amount of space available for expansion (8GB Lite, 10 x Physical for MAX)
    val minFreeSpaceGoalMB: Long = 2_000L,  //Minimum amount of space to try to keep available
    val physicalMB: Long = 64_000L,  //Amount of physical space available
    val status: String = "",  //Status, as display string
    val keepFreeOptionIdx: Int = 0,  //Index of keep free option selected by user
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.NOT_SUBSCRIBED,  //Subscription status
)
{
    enum class SubscriptionStatus {
        NOT_SUBSCRIBED,
        SUBSCRIBED,
    }
}