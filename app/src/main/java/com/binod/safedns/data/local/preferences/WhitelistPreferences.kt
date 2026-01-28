package com.binod.safedns.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("whitelist", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WHITELISTED_DOMAINS = "whitelisted_domains"
    }

    fun addDomain(domain: String) {
        val domains = getWhitelistedDomains().toMutableSet()
        domains.add(domain.trim().lowercase())
        saveDomains(domains)
    }

    fun removeDomain(domain: String) {
        val domains = getWhitelistedDomains().toMutableSet()
        domains.remove(domain.trim().lowercase())
        saveDomains(domains)
    }

    fun isWhitelisted(domain: String): Boolean {
        val cleanDomain = domain.trim().lowercase()
        val whitelisted = getWhitelistedDomains()

        // Check exact match
        if (whitelisted.contains(cleanDomain)) {
            return true
        }

        // Check if any parent domain is whitelisted
        val parts = cleanDomain.split('.')
        for (i in 0 until parts.size - 1) {
            val parentDomain = parts.subList(i, parts.size).joinToString(".")
            if (whitelisted.contains(parentDomain)) {
                return true
            }
        }

        return false
    }

    fun getWhitelistedDomains(): Set<String> {
        return prefs.getStringSet(KEY_WHITELISTED_DOMAINS, emptySet()) ?: emptySet()
    }

    private fun saveDomains(domains: Set<String>) {
        prefs.edit().putStringSet(KEY_WHITELISTED_DOMAINS, domains).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}