package com.binod.safedns.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binod.safedns.data.local.preferences.CustomProfilePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomProfileViewModel @Inject constructor(
    private val customProfilePreferences: CustomProfilePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomProfileUiState())
    val uiState: StateFlow<CustomProfileUiState> = _uiState.asStateFlow()

    init {
        loadConfiguration()
    }

    private fun loadConfiguration() {
        viewModelScope.launch {
            val primary = customProfilePreferences.getPrimaryDns()
            val secondary = customProfilePreferences.getSecondaryDns()
            val urls = customProfilePreferences.getBlocklistUrls()
            val domains = customProfilePreferences.getCustomDomains()

            _uiState.update {
                it.copy(
                    primaryDns = primary,
                    secondaryDns = secondary,
                    blocklistUrls = urls,
                    customDomains = domains.joinToString("\n")
                )
            }
        }
    }

    fun updateDnsServers(primary: String, secondary: String) {
        _uiState.update {
            it.copy(primaryDns = primary, secondaryDns = secondary)
        }
    }

    fun addBlocklistUrl(url: String) {
        _uiState.update {
            it.copy(blocklistUrls = it.blocklistUrls + url)
        }
    }

    fun removeBlocklistUrl(url: String) {
        _uiState.update {
            it.copy(blocklistUrls = it.blocklistUrls - url)
        }
    }

    fun updateCustomDomains(domains: String) {
        _uiState.update { it.copy(customDomains = domains) }
    }

    fun toggleShowAllUrls() {
        _uiState.update { it.copy(showAllUrls = !it.showAllUrls) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun getFilteredUrls(): List<String> {
        val state = _uiState.value
        return if (state.searchQuery.isBlank()) {
            state.blocklistUrls
        } else {
            state.blocklistUrls.filter {
                it.contains(state.searchQuery, ignoreCase = true)
            }
        }
    }

    fun getDisplayUrls(): List<String> {
        val filtered = getFilteredUrls()
        return if (_uiState.value.showAllUrls || filtered.size <= 3) {
            filtered
        } else {
            filtered.take(3)
        }
    }

    fun saveConfiguration() {
        viewModelScope.launch {
            val state = _uiState.value

            customProfilePreferences.savePrimaryDns(state.primaryDns)
            customProfilePreferences.saveSecondaryDns(state.secondaryDns)
            customProfilePreferences.saveBlocklistUrls(state.blocklistUrls)

            val domainList = state.customDomains
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            customProfilePreferences.saveCustomDomains(domainList)
        }
    }
}