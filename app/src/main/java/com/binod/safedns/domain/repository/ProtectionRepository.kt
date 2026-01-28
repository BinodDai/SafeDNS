package com.binod.safedns.domain.repository

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.binod.safedns.data.local.preferences.BlocklistPreferences
import com.binod.safedns.data.local.preferences.PreferencesManager
import com.binod.safedns.domain.model.ProtectionProfile
import com.binod.safedns.domain.model.ProtectionState
import com.binod.safedns.domain.model.ProtectionStatus
import com.binod.safedns.service.AdBlockVpnService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface ProtectionRepository {
    val status: Flow<ProtectionStatus>
    suspend fun start(profile: ProtectionProfile)
    suspend fun stop()
    suspend fun changeProfile(profile: ProtectionProfile)
    fun getBlockedDomainCount(): Int
    fun prepareVpn(): Intent?
}

@Singleton
class ProtectionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val blocklistPreferences: BlocklistPreferences
) : ProtectionRepository {

    private val _status = MutableStateFlow(ProtectionStatus(state = ProtectionState.OFF))
    override val status: Flow<ProtectionStatus> = _status.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var autoUpdateJob: Job? = null
    private var startTimeMillis: Long = 0L

    private val AUTO_UPDATE_INTERVAL = 24 * 60 * 60 * 1000L

    override fun prepareVpn(): Intent? {
        return VpnService.prepare(context)
    }

    override suspend fun start(profile: ProtectionProfile) {
        _status.value = ProtectionStatus(state = ProtectionState.CONNECTING)

        // Apply profile settings and load blocklists
        applyProfileSettings(profile)

        delay(600)

        // Start VPN service
        val intent = Intent(context, AdBlockVpnService::class.java).apply {
            action = AdBlockVpnService.ACTION_START
            val (primary, secondary) = profile.getUpstreamDns()
            putExtra(AdBlockVpnService.EXTRA_PRIMARY_DNS, primary)
            putExtra(AdBlockVpnService.EXTRA_SECONDARY_DNS, secondary)
        }

        context.startService(intent)

        startTimeMillis = System.currentTimeMillis()
        _status.value = ProtectionStatus(state = ProtectionState.ON, durationSeconds = 0L)

        startDurationTimer()

        if (preferencesManager.getAutoUpdate()) {
            startAutoUpdate(profile)
        }

        Log.d("ProtectionRepo", "VPN started with profile: ${profile.name}")
    }

    override suspend fun stop() {
        stopDurationTimer()
        stopAutoUpdate()

        // Stop VPN service
        val intent = Intent(context, AdBlockVpnService::class.java).apply {
            action = AdBlockVpnService.ACTION_STOP
        }
        context.startService(intent)

        _status.value = ProtectionStatus(state = ProtectionState.OFF, durationSeconds = 0L)

        Log.d("ProtectionRepo", "VPN stopped")
    }

    override suspend fun changeProfile(profile: ProtectionProfile) {
        val wasConnected = _status.value.state == ProtectionState.ON

        if (wasConnected) {
            stop()
            delay(300)
            start(profile)
        } else {
            // Save for next connection
            preferencesManager.saveSelectedProfile(profile)
            applyProfileSettings(profile)
        }
    }

    override fun getBlockedDomainCount(): Int {
        return blocklistPreferences.getDomainCount()
    }

    private suspend fun applyProfileSettings(profile: ProtectionProfile) =
        withContext(Dispatchers.IO) {
            try {
                // 1. Load blocklists
                loadBlocklists(profile)

                // 2. Configure DNS
                val (primary, secondary) = profile.getUpstreamDns()
                val label = profile.getUpstreamLabel()
                preferencesManager.saveDnsConfig(primary, secondary, label)

                // 3. Save profile
                preferencesManager.saveSelectedProfile(profile)

                Log.d("ProtectionRepo", "Applied settings for ${profile.name}")
            } catch (e: Exception) {
                Log.e("ProtectionRepo", "Error applying profile settings", e)
            }
        }

    private fun loadBlocklists(profile: ProtectionProfile) {
        val sources = profile.getBlocklistSources()

        if (sources.isEmpty() && profile != ProtectionProfile.CUSTOM) return

        val blockedDomains = mutableSetOf<String>()

        sources.forEach { url ->
            try {
                val content = URL(url).readText()
                content.lines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        val parts = trimmed.split(Regex("\\s+"))
                        if (parts.size >= 2) {
                            blockedDomains.add(parts[1])
                        } else if (parts.size == 1 && parts[0].contains(".")) {
                            // Handle domain-only format
                            blockedDomains.add(parts[0])
                        }
                    }
                }
                Log.d("ProtectionRepo", "Loaded from $url: ${blockedDomains.size} domains")
            } catch (e: Exception) {
                Log.e("ProtectionRepo", "Error loading from $url", e)
            }
        }

        // Add custom domains if using custom profile
        if (profile == ProtectionProfile.CUSTOM) {
            val customDomains =
                profile.getCustomDomains(null) // You'll need to pass CustomProfilePreferences
            blockedDomains.addAll(customDomains)
        }

        blocklistPreferences.saveBlockedDomains(blockedDomains)
        Log.d("ProtectionRepo", "Total blocked domains: ${blockedDomains.size}")
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                val elapsedSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000
                _status.value = ProtectionStatus(
                    state = ProtectionState.ON,
                    durationSeconds = elapsedSeconds
                )
            }
        }
    }

    private fun stopDurationTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun startAutoUpdate(profile: ProtectionProfile) {
        stopAutoUpdate()
        autoUpdateJob = scope.launch {
            while (true) {
                delay(AUTO_UPDATE_INTERVAL) // Wait 24 hours

                if (_status.value.state == ProtectionState.ON &&
                    preferencesManager.getAutoUpdate()
                ) {

                    Log.d("ProtectionRepo", "Auto-updating blocklists...")

                    try {
                        // Reload blocklists in background
                        withContext(Dispatchers.IO) {
                            loadBlocklists(profile)
                        }

                        Log.d(
                            "ProtectionRepo",
                            "Auto-update completed. New count: ${getBlockedDomainCount()}"
                        )
                    } catch (e: Exception) {
                        Log.e("ProtectionRepo", "Auto-update failed", e)
                    }
                }
            }
        }
    }

    private fun stopAutoUpdate() {
        autoUpdateJob?.cancel()
        autoUpdateJob = null
    }

    fun onDestroy() {
        stopDurationTimer()
        stopAutoUpdate()
        scope.cancel()
    }
}