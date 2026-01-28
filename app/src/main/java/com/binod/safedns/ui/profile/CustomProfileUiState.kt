package com.binod.safedns.ui.profile

data class CustomProfileUiState(
    val primaryDns: String = "1.1.1.1",
    val secondaryDns: String = "8.8.8.8",
    val blocklistUrls: List<String> = emptyList(),
    val customDomains: String = "",
    val showAllUrls: Boolean = false,
    val searchQuery: String = ""
)
