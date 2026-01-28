package com.binod.safedns.domain.model

import com.binod.safedns.data.local.preferences.CustomProfilePreferences

enum class ProtectionState { OFF, CONNECTING, ON }
enum class ProtectionProfile {
    BALANCED, STRICT, CUSTOM;

    fun getBlocklistSources(customPrefs: CustomProfilePreferences? = null): List<String> {
        return when (this) {
            BALANCED -> listOf(
                "https://adaway.org/hosts.txt",
                "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
            )

            STRICT -> listOf(
                "https://adaway.org/hosts.txt",
                "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                "https://someonewhocares.org/hosts/zero/hosts",
                "https://winhelp2002.mvps.org/hosts.txt"
            )

            CUSTOM -> customPrefs?.getBlocklistUrls() ?: emptyList()
        }
    }

    fun getUpstreamDns(customPrefs: CustomProfilePreferences? = null): Pair<String, String> {
        return when (this) {
            BALANCED -> Pair("1.1.1.1", "8.8.8.8") // Cloudflare + Google
            STRICT -> Pair("1.1.1.3", "1.0.0.3") // Cloudflare for Families
            CUSTOM -> {
                val primary = customPrefs?.getPrimaryDns() ?: "1.1.1.1"
                val secondary = customPrefs?.getSecondaryDns() ?: "8.8.8.8"
                Pair(primary, secondary)
            }
        }
    }

    fun getUpstreamLabel(): String {
        return when (this) {
            BALANCED -> "Cloudflare & Google"
            STRICT -> "Cloudflare for Families"
            CUSTOM -> "Custom DNS"
        }
    }

    fun getCustomDomains(customPrefs: CustomProfilePreferences?): Set<String> {
        return if (this == CUSTOM) {
            customPrefs?.getCustomDomains() ?: emptySet()
        } else {
            emptySet()
        }
    }

}

data class ProtectionStatus(
    val state: ProtectionState,
    val durationSeconds: Long = 0L
)

