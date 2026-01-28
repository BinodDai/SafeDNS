package com.binod.safedns.ui.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binod.safedns.data.local.preferences.PreferencesManager
import com.binod.safedns.domain.model.ProtectionProfile
import com.binod.safedns.domain.model.ProtectionState
import com.binod.safedns.domain.repository.ProtectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProtectionRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var blockCountRefreshJob: Job? = null
    private var startTimeMillis: Long = 0L

    // VPN permission result handler
    private val _vpnPermissionNeeded = MutableStateFlow<Intent?>(null)
    val vpnPermissionNeeded: StateFlow<Intent?> = _vpnPermissionNeeded.asStateFlow()

    init {
        // Load saved preferences
        loadPreferences()
        refreshBlockedCount()

        viewModelScope.launch {
            repository.status.collect { status ->
                _uiState.update { old ->
                    old.copy(state = status.state)
                }

                when (status.state) {
                    ProtectionState.ON -> {
                        if (timerJob == null) {
                            startTimeMillis = System.currentTimeMillis()
                            startTimer()
                        }
                        startBlockCountRefresh()
                    }

                    ProtectionState.OFF, ProtectionState.CONNECTING -> {
                        stopTimer()
                        stopBlockCountRefresh()
                    }
                }
            }
        }
    }

    private fun loadPreferences() {
        val savedProfile = preferencesManager.getSelectedProfile()
        val autoUpdate = preferencesManager.getAutoUpdate()
        val dnsLabel = preferencesManager.getDnsLabel()

        _uiState.update {
            it.copy(
                profile = savedProfile,
                upstreamLabel = dnsLabel,
                autoUpdateFilters = autoUpdate
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsedSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000
                _uiState.update { it.copy(durationText = formatDuration(elapsedSeconds)) }
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(durationText = "00:00:00") }
    }

    private fun startBlockCountRefresh() {
        stopBlockCountRefresh()
        blockCountRefreshJob = viewModelScope.launch {
            while (true) {
                delay(60_000) // Refresh every 1 minute
                refreshBlockedCount()
            }
        }
    }

    private fun stopBlockCountRefresh() {
        blockCountRefreshJob?.cancel()
        blockCountRefreshJob = null
    }

    fun onToggleProtection() {
        viewModelScope.launch {
            when (_uiState.value.state) {
                ProtectionState.OFF -> {
                    // Check VPN permission
                    val permissionIntent = repository.prepareVpn()
                    if (permissionIntent != null) {
                        _vpnPermissionNeeded.value = permissionIntent
                    } else {
                        // Permission already granted
                        repository.start(_uiState.value.profile)
                    }
                }
                ProtectionState.CONNECTING -> repository.stop()
                ProtectionState.ON -> repository.stop()
            }
        }
    }

    fun onVpnPermissionGranted() {
        viewModelScope.launch {
            _vpnPermissionNeeded.value = null
            repository.start(_uiState.value.profile)
        }
    }

    fun onVpnPermissionDenied() {
        _vpnPermissionNeeded.value = null
        _uiState.update { it.copy(state = ProtectionState.OFF) }
    }

    fun onSelectProfile(profile: ProtectionProfile) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    profile = profile,
                    upstreamLabel = profile.getUpstreamLabel()
                )
            }

            if (_uiState.value.state == ProtectionState.ON) {
                repository.changeProfile(profile)
                refreshBlockedCount()
            } else {
                preferencesManager.saveSelectedProfile(profile)
                refreshBlockedCount()
            }
        }
    }

    fun onToggleAutoUpdate(value: Boolean) {
        _uiState.update { it.copy(autoUpdateFilters = value) }
        preferencesManager.saveAutoUpdate(value)

        // If turning on while connected, restart to enable auto-update
        if (value && _uiState.value.state == ProtectionState.ON) {
            viewModelScope.launch {
                val currentProfile = _uiState.value.profile
                repository.stop()
                delay(300)
                repository.start(currentProfile)
            }
        }
    }

    private fun formatDuration(seconds: Long): String {
        val h = TimeUnit.SECONDS.toHours(seconds)
        val m = TimeUnit.SECONDS.toMinutes(seconds) % 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private fun refreshBlockedCount() {
        val count = repository.getBlockedDomainCount()
        _uiState.update { it.copy(blockedDomains = count) }
    }


    override fun onCleared() {
        super.onCleared()
        stopTimer()
        stopBlockCountRefresh()
    }
}