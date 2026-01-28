package com.binod.safedns.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsResolver(
    private val primaryDns: String,
    private val secondaryDns: String
) {
    companion object {
        private const val TAG = "DnsResolver"
        private const val DNS_PORT = 53
        private const val TIMEOUT = 5000 // 5 seconds
    }

    private val cache = mutableMapOf<String, DnsCacheEntry>()
    private val cacheLock = Any()

    data class DnsCacheEntry(
        val response: ByteArray,
        val timestamp: Long,
        val ttl: Long = 300000 // 5 minutes default
    )

    suspend fun resolve(dnsQuery: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // Try primary DNS first
            resolveWithServer(dnsQuery, primaryDns) ?: run {
                // Fallback to secondary DNS
                Log.d(TAG, "Primary DNS failed, trying secondary")
                resolveWithServer(dnsQuery, secondaryDns)
            }
        } catch (e: Exception) {
            Log.e(TAG, "DNS resolution failed", e)
            null
        }
    }

    private fun resolveWithServer(query: ByteArray, dnsServer: String): ByteArray? {
        var socket: DatagramSocket? = null

        try {
            socket = DatagramSocket()
            socket.soTimeout = TIMEOUT

            val dnsAddress = InetAddress.getByName(dnsServer)
            val sendPacket = DatagramPacket(query, query.size, dnsAddress, DNS_PORT)
            socket.send(sendPacket)

            val responseBuffer = ByteArray(512)
            val receivePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(receivePacket)

            val response = responseBuffer.copyOf(receivePacket.length)

            Log.d(TAG, "DNS query resolved via $dnsServer")
            return response

        } catch (e: Exception) {
            Log.e(TAG, "Error resolving with $dnsServer", e)
            return null
        } finally {
            socket?.close()
        }
    }

    fun getCachedResponse(domain: String): ByteArray? {
        synchronized(cacheLock) {
            val entry = cache[domain] ?: return null

            // Check if cache entry is still valid
            if (System.currentTimeMillis() - entry.timestamp > entry.ttl) {
                cache.remove(domain)
                return null
            }

            return entry.response
        }
    }

    fun cacheResponse(domain: String, response: ByteArray) {
        synchronized(cacheLock) {
            cache[domain] = DnsCacheEntry(response, System.currentTimeMillis())

            // Limit cache size
            if (cache.size > 1000) {
                val oldest = cache.entries.minByOrNull { it.value.timestamp }
                oldest?.let { cache.remove(it.key) }
            }
        }
    }

    fun clearCache() {
        synchronized(cacheLock) {
            cache.clear()
        }
    }
}