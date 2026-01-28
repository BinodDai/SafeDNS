package com.binod.safedns.domain.model

data class DnsQuery(
    val id: Long = System.currentTimeMillis(),
    val domain: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBlocked: Boolean,
    val appPackage: String? = null,
    val queryType: String = "A" // A, AAAA, etc.
)

data class DnsStats(
    val totalBlocked: Int = 0,
    val totalAllowed: Int = 0,
    val todayBlocked: Int = 0,
    val todayAllowed: Int = 0,
    val topBlockedDomains: List<BlockedDomainStat> = emptyList(),
    val hourlyStats: List<HourlyStat> = emptyList(),
    val dataSaved: Long = 0 // in bytes
) {
    val totalQueries: Int
        get() = totalBlocked + totalAllowed

    val blockPercentage: Float
        get() = if (totalQueries > 0) (totalBlocked.toFloat() / totalQueries * 100) else 0f
}

data class BlockedDomainStat(
    val domain: String,
    val count: Int
)

data class HourlyStat(
    val hour: Int, // 0-23
    val blocked: Int,
    val allowed: Int
)