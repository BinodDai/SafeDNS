package com.binod.safedns.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import com.binod.safedns.domain.model.ProtectionProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SELECTED_PROFILE = "selected_profile"
        private const val KEY_AUTO_UPDATE = "auto_update_filters"
        private const val KEY_PRIMARY_DNS = "primary_dns"
        private const val KEY_SECONDARY_DNS = "secondary_dns"
        private const val KEY_DNS_LABEL = "dns_label"
    }

    // Profile preferences
    fun saveSelectedProfile(profile: ProtectionProfile) {
        prefs.edit().putString(KEY_SELECTED_PROFILE, profile.name).apply()
    }

    fun getSelectedProfile(): ProtectionProfile {
        val profileName = prefs.getString(KEY_SELECTED_PROFILE, ProtectionProfile.BALANCED.name)
        return try {
            ProtectionProfile.valueOf(profileName ?: ProtectionProfile.BALANCED.name)
        } catch (e: IllegalArgumentException) {
            ProtectionProfile.BALANCED
        }
    }

    // Auto-update preferences
    fun saveAutoUpdate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
    }

    fun getAutoUpdate(): Boolean {
        return prefs.getBoolean(KEY_AUTO_UPDATE, false)
    }

    // DNS preferences
    fun saveDnsConfig(primary: String, secondary: String, label: String) {
        prefs.edit()
            .putString(KEY_PRIMARY_DNS, primary)
            .putString(KEY_SECONDARY_DNS, secondary)
            .putString(KEY_DNS_LABEL, label)
            .apply()
    }

    fun getPrimaryDns(): String {
        return prefs.getString(KEY_PRIMARY_DNS, "1.1.1.1") ?: "1.1.1.1"
    }

    fun getSecondaryDns(): String {
        return prefs.getString(KEY_SECONDARY_DNS, "8.8.8.8") ?: "8.8.8.8"
    }

    fun getDnsLabel(): String {
        return prefs.getString(KEY_DNS_LABEL, "Cloudflare & Google") ?: "Cloudflare & Google"
    }
}
