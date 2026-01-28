package com.binod.safedns.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import com.binod.safedns.domain.model.DnsQuery
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dns_stats", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOTAL_BLOCKED = "total_blocked"
        private const val KEY_TOTAL_ALLOWED = "total_allowed"
        private const val KEY_TODAY_BLOCKED = "today_blocked"
        private const val KEY_TODAY_ALLOWED = "today_allowed"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_TOP_BLOCKED = "top_blocked_domains"
        private const val KEY_HOURLY_STATS = "hourly_stats"
        private const val KEY_DATA_SAVED = "data_saved_bytes"
        private const val KEY_RECENT_LOGS = "recent_logs"
        private const val MAX_LOGS = 500
        private const val MAX_TOP_DOMAINS = 10
    }

    private val _recentQueries = MutableStateFlow<List<DnsQuery>>(emptyList())
    val recentQueries: StateFlow<List<DnsQuery>> = _recentQueries.asStateFlow()

    init {
        checkAndResetDaily()
        loadRecentLogs()
    }

    // Stats methods
    fun incrementBlocked() {
        checkAndResetDaily()
        prefs.edit()
            .putInt(KEY_TOTAL_BLOCKED, getTotalBlocked() + 1)
            .putInt(KEY_TODAY_BLOCKED, getTodayBlocked() + 1)
            .putLong(KEY_DATA_SAVED, getDataSaved() + 50000) // Estimate 50KB per ad
            .apply()
    }

    fun incrementAllowed() {
        checkAndResetDaily()
        prefs.edit()
            .putInt(KEY_TOTAL_ALLOWED, getTotalAllowed() + 1)
            .putInt(KEY_TODAY_ALLOWED, getTodayAllowed() + 1)
            .apply()
    }

    fun getTotalBlocked(): Int = prefs.getInt(KEY_TOTAL_BLOCKED, 0)
    fun getTotalAllowed(): Int = prefs.getInt(KEY_TOTAL_ALLOWED, 0)
    fun getTodayBlocked(): Int = prefs.getInt(KEY_TODAY_BLOCKED, 0)
    fun getTodayAllowed(): Int = prefs.getInt(KEY_TODAY_ALLOWED, 0)
    fun getDataSaved(): Long = prefs.getLong(KEY_DATA_SAVED, 0)

    fun addTopBlockedDomain(domain: String) {
        val topDomains = getTopBlockedDomains().toMutableMap()
        topDomains[domain] = (topDomains[domain] ?: 0) + 1

        // Keep only top 10
        val sorted = topDomains.entries
            .sortedByDescending { it.value }
            .take(MAX_TOP_DOMAINS)
            .associate { it.key to it.value }

        val json = JSONObject(sorted).toString()
        prefs.edit().putString(KEY_TOP_BLOCKED, json).apply()
    }

    fun getTopBlockedDomains(): Map<String, Int> {
        val jsonString = prefs.getString(KEY_TOP_BLOCKED, "{}") ?: "{}"
        val json = JSONObject(jsonString)
        val map = mutableMapOf<String, Int>()

        json.keys().forEach { key ->
            map[key] = json.getInt(key)
        }

        return map
    }

    fun updateHourlyStat(isBlocked: Boolean) {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val hourlyStats = getHourlyStats().toMutableMap()

        val current = hourlyStats[currentHour] ?: Pair(0, 0)
        hourlyStats[currentHour] = if (isBlocked) {
            Pair(current.first + 1, current.second)
        } else {
            Pair(current.first, current.second + 1)
        }

        val json = JSONObject()
        hourlyStats.forEach { (hour, stats) ->
            json.put(hour.toString(), JSONArray().put(stats.first).put(stats.second))
        }

        prefs.edit().putString(KEY_HOURLY_STATS, json.toString()).apply()
    }

    fun getHourlyStats(): Map<Int, Pair<Int, Int>> {
        val jsonString = prefs.getString(KEY_HOURLY_STATS, "{}") ?: "{}"
        val json = JSONObject(jsonString)
        val map = mutableMapOf<Int, Pair<Int, Int>>()

        json.keys().forEach { key ->
            val array = json.getJSONArray(key)
            map[key.toInt()] = Pair(array.getInt(0), array.getInt(1))
        }

        return map
    }

    // Query logs
    fun addQueryLog(query: DnsQuery) {
        val logs = _recentQueries.value.toMutableList()
        logs.add(0, query) // Add to beginning

        // Keep only last MAX_LOGS
        if (logs.size > MAX_LOGS) {
            logs.removeAt(logs.size - 1)
        }

        _recentQueries.value = logs
        saveLogsToPrefs(logs)

        // Update stats
        if (query.isBlocked) {
            incrementBlocked()
            addTopBlockedDomain(query.domain)
        } else {
            incrementAllowed()
        }

        updateHourlyStat(query.isBlocked)
    }

    private fun saveLogsToPrefs(logs: List<DnsQuery>) {
        val jsonArray = JSONArray()
        logs.take(100).forEach { query -> // Save only last 100 to prefs
            val json = JSONObject().apply {
                put("id", query.id)
                put("domain", query.domain)
                put("timestamp", query.timestamp)
                put("isBlocked", query.isBlocked)
                put("appPackage", query.appPackage ?: "")
                put("queryType", query.queryType)
            }
            jsonArray.put(json)
        }

        prefs.edit().putString(KEY_RECENT_LOGS, jsonArray.toString()).apply()
    }

    private fun loadRecentLogs() {
        val jsonString = prefs.getString(KEY_RECENT_LOGS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val logs = mutableListOf<DnsQuery>()

        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            logs.add(
                DnsQuery(
                    id = json.getLong("id"),
                    domain = json.getString("domain"),
                    timestamp = json.getLong("timestamp"),
                    isBlocked = json.getBoolean("isBlocked"),
                    appPackage = json.getString("appPackage").takeIf { it.isNotEmpty() },
                    queryType = json.getString("queryType")
                )
            )
        }

        _recentQueries.value = logs
    }

    fun clearLogs() {
        _recentQueries.value = emptyList()
        prefs.edit().remove(KEY_RECENT_LOGS).apply()
    }

    fun resetStats() {
        prefs.edit()
            .putInt(KEY_TOTAL_BLOCKED, 0)
            .putInt(KEY_TOTAL_ALLOWED, 0)
            .putInt(KEY_TODAY_BLOCKED, 0)
            .putInt(KEY_TODAY_ALLOWED, 0)
            .putLong(KEY_DATA_SAVED, 0)
            .putString(KEY_TOP_BLOCKED, "{}")
            .putString(KEY_HOURLY_STATS, "{}")
            .apply()
    }

    private fun checkAndResetDaily() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastReset = prefs.getInt(KEY_LAST_RESET_DATE, -1)

        if (lastReset != today) {
            // Reset daily stats
            prefs.edit()
                .putInt(KEY_TODAY_BLOCKED, 0)
                .putInt(KEY_TODAY_ALLOWED, 0)
                .putInt(KEY_LAST_RESET_DATE, today)
                .putString(KEY_HOURLY_STATS, "{}") // Reset hourly stats
                .apply()
        }
    }
}