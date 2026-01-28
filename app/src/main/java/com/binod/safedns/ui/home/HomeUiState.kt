package com.binod.safedns.ui.home

import com.binod.safedns.domain.model.ProtectionProfile
import com.binod.safedns.domain.model.ProtectionState


data class HomeUiState(
    val state: ProtectionState = ProtectionState.OFF,
    val durationText: String = "00:00:00",
    val profile: ProtectionProfile = ProtectionProfile.BALANCED,
    val upstreamLabel: String = "Cloudflare & Google",
    val autoUpdateFilters: Boolean = false,
    val blockedDomains: Int = 0
)
