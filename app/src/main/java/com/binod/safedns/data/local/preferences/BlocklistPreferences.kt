package com.binod.safedns.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("blocklists", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BLOCKED_DOMAINS = "blocked_domains"
        private const val KEY_DOMAIN_COUNT = "domain_count"
        private const val KEY_LAST_UPDATE = "last_update"
    }

    fun saveBlockedDomains(domains: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_BLOCKED_DOMAINS, domains)
            .putInt(KEY_DOMAIN_COUNT, domains.size)
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    fun getBlockedDomains(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_DOMAINS, emptySet()) ?: emptySet()
    }

    fun getDomainCount(): Int {
        return prefs.getInt(KEY_DOMAIN_COUNT, 0)
    }

    fun getLastUpdateTime(): Long {
        return prefs.getLong(KEY_LAST_UPDATE, 0L)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}