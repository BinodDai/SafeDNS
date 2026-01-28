package com.binod.safedns.service

import android.util.Log
import com.binod.safedns.data.local.preferences.BlocklistPreferences
import com.binod.safedns.data.local.preferences.CustomProfilePreferences
import com.binod.safedns.data.local.preferences.StatsPreferences
import com.binod.safedns.data.local.preferences.WhitelistPreferences
import com.binod.safedns.domain.model.DnsQuery
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer

class PacketHandler(
    private val blocklistPreferences: BlocklistPreferences,
    private val customProfilePreferences: CustomProfilePreferences,
    private val whitelistPreferences: WhitelistPreferences,
    private val statsPreferences: StatsPreferences,
    private val onQueryProcessed: (blocked: Boolean) -> Unit
) {
    companion object {
        private const val TAG = "PacketHandler"
        private const val IPV4_HEADER_SIZE = 20
        private const val IPV6_HEADER_SIZE = 40
        private const val UDP_HEADER_SIZE = 8
        private const val DNS_PORT = 53
        private const val PROTOCOL_UDP = 17
        private const val IPV6_NEXT_HEADER_UDP = 17
    }

    fun handlePacket(packet: ByteArray, length: Int, dnsResolver: DnsResolver): ByteArray? {
        if (length < IPV4_HEADER_SIZE + UDP_HEADER_SIZE) {
            return null
        }

        try {
            val buffer = ByteBuffer.wrap(packet, 0, length)
            val version = (buffer.get(0).toInt() and 0xF0) shr 4

            return when (version) {
                4 -> handleIPv4Packet(packet, length, buffer, dnsResolver)
                6 -> handleIPv6Packet(packet, length, buffer, dnsResolver)
                else -> {
                    Log.d(TAG, "Unsupported IP version: $version")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling packet", e)
            return null
        }
    }

    private fun handleIPv4Packet(
        packet: ByteArray,
        length: Int,
        buffer: ByteBuffer,
        dnsResolver: DnsResolver
    ): ByteArray? {
        val ipHeader = parseIPv4Header(buffer) ?: return null

        if (ipHeader.protocol != PROTOCOL_UDP) {
            return null
        }

        val udpHeader = parseUdpHeader(buffer, ipHeader.headerLength) ?: return null

        if (udpHeader.destPort != DNS_PORT && udpHeader.sourcePort != DNS_PORT) {
            return null
        }

        val dnsOffset = ipHeader.headerLength + UDP_HEADER_SIZE
        if (dnsOffset + 12 > length) return null

        val dnsQuery = packet.copyOfRange(dnsOffset, length)
        return processDnsQuery(dnsQuery, packet, length, ipHeader, null, udpHeader, dnsResolver)
    }

    private fun handleIPv6Packet(
        packet: ByteArray,
        length: Int,
        buffer: ByteBuffer,
        dnsResolver: DnsResolver
    ): ByteArray? {
        val ipv6Header = parseIPv6Header(buffer) ?: return null

        if (ipv6Header.nextHeader != IPV6_NEXT_HEADER_UDP) {
            return null
        }

        val udpHeader = parseUdpHeader(buffer, IPV6_HEADER_SIZE) ?: return null

        if (udpHeader.destPort != DNS_PORT && udpHeader.sourcePort != DNS_PORT) {
            return null
        }

        val dnsOffset = IPV6_HEADER_SIZE + UDP_HEADER_SIZE
        if (dnsOffset + 12 > length) return null

        val dnsQuery = packet.copyOfRange(dnsOffset, length)
        return processDnsQuery(dnsQuery, packet, length, null, ipv6Header, udpHeader, dnsResolver)
    }

    private fun processDnsQuery(
        dnsQuery: ByteArray,
        originalPacket: ByteArray,
        length: Int,
        ipv4Header: IPv4Header?,
        ipv6Header: IPv6Header?,
        udpHeader: UdpHeader,
        dnsResolver: DnsResolver
    ): ByteArray? {
        val domain = parseDomainFromDnsQuery(dnsQuery) ?: return null

        Log.d(TAG, "DNS query for: $domain (${if (ipv4Header != null) "IPv4" else "IPv6"})")

        // Check whitelist first
        if (whitelistPreferences.isWhitelisted(domain)) {
            Log.d(TAG, "Domain whitelisted: $domain")
            statsPreferences.addQueryLog(
                DnsQuery(
                    domain = domain,
                    isBlocked = false,
                    queryType = "A"
                )
            )
            onQueryProcessed(false)

            return runBlocking {
                val response = dnsResolver.resolve(dnsQuery)
                if (response != null) {
                    if (ipv4Header != null) {
                        wrapIPv4DnsResponse(response, ipv4Header, udpHeader)
                    } else {
                        wrapIPv6DnsResponse(response, ipv6Header!!, udpHeader)
                    }
                } else {
                    null
                }
            }
        }

        // Check if domain should be blocked
        if (shouldBlockDomain(domain)) {
            Log.d(TAG, "Blocking domain: $domain")
            statsPreferences.addQueryLog(
                DnsQuery(
                    domain = domain,
                    isBlocked = true,
                    queryType = "A"
                )
            )
            onQueryProcessed(true)
            return if (ipv4Header != null) {
                createIPv4BlockedResponse(originalPacket, length, ipv4Header, udpHeader)
            } else {
                createIPv6BlockedResponse(originalPacket, length, ipv6Header!!, udpHeader)
            }
        }

        // Log allowed query
        statsPreferences.addQueryLog(
            DnsQuery(
                domain = domain,
                isBlocked = false,
                queryType = "A"
            )
        )

        // Allow the query
        onQueryProcessed(false)
        return runBlocking {
            val response = dnsResolver.resolve(dnsQuery)
            if (response != null) {
                if (ipv4Header != null) {
                    wrapIPv4DnsResponse(response, ipv4Header, udpHeader)
                } else {
                    wrapIPv6DnsResponse(response, ipv6Header!!, udpHeader)
                }
            } else {
                null
            }
        }
    }

    private fun parseIPv4Header(buffer: ByteBuffer): IPv4Header? {
        try {
            val versionAndIhl = buffer.get(0).toInt() and 0xFF
            val version = (versionAndIhl shr 4) and 0x0F

            if (version != 4) return null

            val headerLength = (versionAndIhl and 0x0F) * 4
            val protocol = buffer.get(9).toInt() and 0xFF

            val sourceIp = ByteArray(4)
            buffer.position(12)
            buffer.get(sourceIp)

            val destIp = ByteArray(4)
            buffer.get(destIp)

            return IPv4Header(headerLength, protocol, sourceIp, destIp)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing IPv4 header", e)
            return null
        }
    }

    private fun parseIPv6Header(buffer: ByteBuffer): IPv6Header? {
        try {
            val versionTrafficClass = buffer.getInt(0)
            val version = (versionTrafficClass shr 28) and 0x0F

            if (version != 6) return null

            buffer.position(6)
            val nextHeader = buffer.get().toInt() and 0xFF
            val hopLimit = buffer.get().toInt() and 0xFF

            val sourceIp = ByteArray(16)
            buffer.get(sourceIp)

            val destIp = ByteArray(16)
            buffer.get(destIp)

            return IPv6Header(nextHeader, hopLimit, sourceIp, destIp)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing IPv6 header", e)
            return null
        }
    }

    private fun parseUdpHeader(buffer: ByteBuffer, ipHeaderLength: Int): UdpHeader? {
        try {
            buffer.position(ipHeaderLength)

            val sourcePort = buffer.short.toInt() and 0xFFFF
            val destPort = buffer.short.toInt() and 0xFFFF
            val length = buffer.short.toInt() and 0xFFFF
            val checksum = buffer.short.toInt() and 0xFFFF

            return UdpHeader(sourcePort, destPort, length, checksum)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing UDP header", e)
            return null
        }
    }

    private fun parseDomainFromDnsQuery(dnsQuery: ByteArray): String? {
        if (dnsQuery.size < 13) return null

        try {
            val sb = StringBuilder()
            var pos = 12 // Skip DNS header

            while (pos < dnsQuery.size) {
                val length = dnsQuery[pos].toInt() and 0xFF
                if (length == 0) break

                pos++
                if (pos + length > dnsQuery.size) break

                if (sb.isNotEmpty()) sb.append('.')

                for (i in 0 until length) {
                    sb.append(dnsQuery[pos + i].toInt().toChar())
                }

                pos += length
            }

            val domain = sb.toString().lowercase()
            return if (domain.isNotEmpty()) domain else null

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing domain", e)
            return null
        }
    }

    private fun shouldBlockDomain(domain: String): Boolean {
        val blocklistDomains = blocklistPreferences.getBlockedDomains()
        val customDomains = customProfilePreferences.getCustomDomains()
        val allBlockedDomains = blocklistDomains + customDomains

        // Check exact match
        if (allBlockedDomains.contains(domain)) {
            return true
        }

        // Check if any parent domain is blocked
        val parts = domain.split('.')
        for (i in 0 until parts.size - 1) {
            val parentDomain = parts.subList(i, parts.size).joinToString(".")
            if (allBlockedDomains.contains(parentDomain)) {
                return true
            }
        }

        return false
    }

    private fun createIPv4BlockedResponse(
        originalPacket: ByteArray,
        length: Int,
        ipHeader: IPv4Header,
        udpHeader: UdpHeader
    ): ByteArray {
        val dnsOffset = ipHeader.headerLength + UDP_HEADER_SIZE
        val dnsQuery = originalPacket.copyOfRange(dnsOffset, length)

        val response = ByteArray(dnsQuery.size)
        System.arraycopy(dnsQuery, 0, response, 0, 12)

        // Set response flags (QR=1, RCODE=3 for NXDOMAIN)
        response[2] = 0x81.toByte()
        response[3] = 0x83.toByte()

        return wrapIPv4DnsResponse(response, ipHeader, udpHeader)
    }

    private fun createIPv6BlockedResponse(
        originalPacket: ByteArray,
        length: Int,
        ipHeader: IPv6Header,
        udpHeader: UdpHeader
    ): ByteArray {
        val dnsOffset = IPV6_HEADER_SIZE + UDP_HEADER_SIZE
        val dnsQuery = originalPacket.copyOfRange(dnsOffset, length)

        val response = ByteArray(dnsQuery.size)
        System.arraycopy(dnsQuery, 0, response, 0, 12)

        // Set response flags (QR=1, RCODE=3 for NXDOMAIN)
        response[2] = 0x81.toByte()
        response[3] = 0x83.toByte()

        return wrapIPv6DnsResponse(response, ipHeader, udpHeader)
    }

    private fun wrapIPv4DnsResponse(
        dnsResponse: ByteArray,
        ipHeader: IPv4Header,
        udpHeader: UdpHeader
    ): ByteArray {
        val totalLength = IPV4_HEADER_SIZE + UDP_HEADER_SIZE + dnsResponse.size
        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // IPv4 header
        buffer.put(0x45.toByte()) // Version 4, IHL 5
        buffer.put(0x00.toByte()) // DSCP and ECN
        buffer.putShort(totalLength.toShort()) // Total length
        buffer.putShort(0) // Identification
        buffer.putShort(0) // Flags and fragment offset
        buffer.put(64.toByte()) // TTL
        buffer.put(PROTOCOL_UDP.toByte()) // Protocol (UDP)
        buffer.putShort(0) // Header checksum (will calculate)
        buffer.put(ipHeader.destIp) // Source IP (swap)
        buffer.put(ipHeader.sourceIp) // Dest IP (swap)

        // UDP header
        buffer.putShort(udpHeader.destPort.toShort()) // Source port (swap)
        buffer.putShort(udpHeader.sourcePort.toShort()) // Dest port (swap)
        buffer.putShort((UDP_HEADER_SIZE + dnsResponse.size).toShort()) // Length
        buffer.putShort(0) // Checksum

        // DNS response
        buffer.put(dnsResponse)

        // Calculate and set IP checksum
        val checksum = calculateIPv4Checksum(packet, IPV4_HEADER_SIZE)
        packet[10] = (checksum shr 8).toByte()
        packet[11] = checksum.toByte()

        return packet
    }

    private fun wrapIPv6DnsResponse(
        dnsResponse: ByteArray,
        ipHeader: IPv6Header,
        udpHeader: UdpHeader
    ): ByteArray {
        val payloadLength = UDP_HEADER_SIZE + dnsResponse.size
        val totalLength = IPV6_HEADER_SIZE + payloadLength
        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // IPv6 header
        buffer.putInt((6 shl 28)) // Version 6, Traffic Class 0, Flow Label 0
        buffer.putShort(payloadLength.toShort()) // Payload length
        buffer.put(IPV6_NEXT_HEADER_UDP.toByte()) // Next header (UDP)
        buffer.put(64.toByte()) // Hop limit
        buffer.put(ipHeader.destIp) // Source IP (swap)
        buffer.put(ipHeader.sourceIp) // Dest IP (swap)

        // UDP header
        buffer.putShort(udpHeader.destPort.toShort()) // Source port (swap)
        buffer.putShort(udpHeader.sourcePort.toShort()) // Dest port (swap)
        buffer.putShort((UDP_HEADER_SIZE + dnsResponse.size).toShort()) // Length
        buffer.putShort(0) // Checksum (optional for IPv6 over UDP for DNS)

        // DNS response
        buffer.put(dnsResponse)

        return packet
    }

    private fun calculateIPv4Checksum(packet: ByteArray, headerLength: Int): Int {
        var sum = 0L

        for (i in 0 until headerLength step 2) {
            if (i == 10) continue // Skip checksum field

            val word = ((packet[i].toInt() and 0xFF) shl 8) or
                    (packet[i + 1].toInt() and 0xFF)
            sum += word
        }

        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return (sum.inv() and 0xFFFF).toInt()
    }

    data class IPv4Header(
        val headerLength: Int,
        val protocol: Int,
        val sourceIp: ByteArray,
        val destIp: ByteArray
    )

    data class IPv6Header(
        val nextHeader: Int,
        val hopLimit: Int,
        val sourceIp: ByteArray,
        val destIp: ByteArray
    )

    data class UdpHeader(
        val sourcePort: Int,
        val destPort: Int,
        val length: Int,
        val checksum: Int
    )
}