package com.binod.safedns.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binod.safedns.data.local.preferences.StatsPreferences
import com.binod.safedns.domain.model.BlockedDomainStat
import com.binod.safedns.domain.model.DnsStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsPreferences: StatsPreferences
) : ViewModel() {

    private val _stats = MutableStateFlow(DnsStats())
    val stats: StateFlow<DnsStats> = _stats.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val topBlockedMap = statsPreferences.getTopBlockedDomains()
            val topBlockedList = topBlockedMap.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { BlockedDomainStat(it.key, it.value) }

            _stats.value = DnsStats(
                totalBlocked = statsPreferences.getTotalBlocked(),
                totalAllowed = statsPreferences.getTotalAllowed(),
                todayBlocked = statsPreferences.getTodayBlocked(),
                todayAllowed = statsPreferences.getTodayAllowed(),
                topBlockedDomains = topBlockedList,
                dataSaved = statsPreferences.getDataSaved()
            )
        }
    }

    fun resetStats() {
        viewModelScope.launch {
            statsPreferences.resetStats()
            loadStats()
        }
    }

    fun refresh() {
        loadStats()
    }
}