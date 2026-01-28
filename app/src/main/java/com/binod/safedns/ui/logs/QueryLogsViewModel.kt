package com.binod.safedns.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binod.safedns.data.local.preferences.StatsPreferences
import com.binod.safedns.data.local.preferences.WhitelistPreferences
import com.binod.safedns.domain.model.DnsQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueryLogsViewModel @Inject constructor(
    private val statsPreferences: StatsPreferences,
    private val whitelistPreferences: WhitelistPreferences
) : ViewModel() {

    val queries: StateFlow<List<DnsQuery>> = statsPreferences.recentQueries

    fun clearLogs() {
        viewModelScope.launch {
            statsPreferences.clearLogs()
        }
    }

    fun addToWhitelist(domain: String) {
        viewModelScope.launch {
            whitelistPreferences.addDomain(domain)
        }
    }
}