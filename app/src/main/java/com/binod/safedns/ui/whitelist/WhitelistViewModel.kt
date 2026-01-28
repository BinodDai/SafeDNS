package com.binod.safedns.ui.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binod.safedns.data.local.preferences.WhitelistPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhitelistViewModel @Inject constructor(
    private val whitelistPreferences: WhitelistPreferences
) : ViewModel() {

    private val _domains = MutableStateFlow<Set<String>>(emptySet())
    val domains: StateFlow<Set<String>> = _domains.asStateFlow()

    init {
        loadDomains()
    }

    private fun loadDomains() {
        viewModelScope.launch {
            _domains.value = whitelistPreferences.getWhitelistedDomains()
        }
    }

    fun addDomain(domain: String) {
        viewModelScope.launch {
            whitelistPreferences.addDomain(domain)
            loadDomains()
        }
    }

    fun removeDomain(domain: String) {
        viewModelScope.launch {
            whitelistPreferences.removeDomain(domain)
            loadDomains()
        }
    }
}