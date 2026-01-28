package com.binod.safedns.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomProfilePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("custom_profile", Context.MODE_PRIVATE)

    fun savePrimaryDns(dns: String) {
        prefs.edit().putString("custom_primary_dns", dns).apply()
    }

    fun saveSecondaryDns(dns: String) {
        prefs.edit().putString("custom_secondary_dns", dns).apply()
    }

    fun getPrimaryDns(): String {
        return prefs.getString("custom_primary_dns", "1.1.1.1") ?: "1.1.1.1"
    }

    fun getSecondaryDns(): String {
        return prefs.getString("custom_secondary_dns", "8.8.8.8") ?: "8.8.8.8"
    }

    fun saveBlocklistUrls(urls: List<String>) {
        prefs.edit().putStringSet("custom_blocklist_urls", urls.toSet()).apply()
    }

    fun getBlocklistUrls(): List<String> {
        return prefs.getStringSet("custom_blocklist_urls", emptySet())?.toList() ?: emptyList()
    }

    fun saveCustomDomains(domains: Set<String>) {
        prefs.edit().putStringSet("custom_domains", domains).apply()
    }

    fun getCustomDomains(): Set<String> {
        return prefs.getStringSet("custom_domains", emptySet()) ?: emptySet()
    }
}