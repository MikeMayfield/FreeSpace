package com.tmf.freespace.presentationlayer.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeScreenVM : ViewModel() {
    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()

    init {
        populateHomeScreenState()
    }

    fun updateStatus(value: HomeScreenState.Status) {
        _uiState.update { it.copy(status = value) }
    }

    fun updateKeepFreeOption(value: HomeScreenState.KeepFreeOption) {
        //TODO Compute KeepFree memory value and store in DB; Start background processing if idle; Keep track of status in DB
        _uiState.update { it.copy(keepFreeOption = value) }
    }

    fun updateSubscriptionStatus(value: HomeScreenState.SubscriptionStatus) {
        //TODO Update status in DB
        _uiState.update { it.copy(subscriptionStatus = value) }
    }

    private fun populateHomeScreenState() {
        //TODO Populate HomeScreenState from DB
    }
}

data class HomeScreenState(
    val photosMB: Long = 0L,
    val videosMB: Long = 0L,
    val appsMB: Long = 0L,
    val addedMB: Long = 0L,
    val maxMB: Long = 0L,
    val physicalMB: Long = 0L,
    val status: Status = Status.IDLE,
    val keepFreeOption: KeepFreeOption = KeepFreeOption.TWO_GB,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.NOT_SUBSCRIBED,
)
{
    enum class Status {
        IDLE,
        PROCESSING,
        BATTERY_LOW,
        AT_LIMIT,
    }

    enum class KeepFreeOption {
        TWO_GB,
        FIVE_GB,
        TEN_GB,
        FIVE_PERCENT,
        TEN_PERCENT,
    }

    enum class SubscriptionStatus {
        NOT_SUBSCRIBED,
        SUBSCRIBED,
    }
}