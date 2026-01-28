package com.binod.safedns.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.binod.safedns.R
import com.binod.safedns.data.local.preferences.BlocklistPreferences
import com.binod.safedns.data.local.preferences.CustomProfilePreferences
import com.binod.safedns.data.local.preferences.StatsPreferences
import com.binod.safedns.data.local.preferences.WhitelistPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import javax.inject.Inject

@AndroidEntryPoint
class AdBlockVpnService : VpnService() {

    companion object {
        private const val TAG = "AdBlockVpnService"
        private const val VPN_MTU = 1500
        private const val VPN_ADDRESS_V4 = "10.0.0.2"
        private const val VPN_ADDRESS_V6 = "fd00::1"
        private const val VPN_ROUTE_V4 = "0.0.0.0"
        private const val VPN_ROUTE_V6 = "::"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "vpn_channel"

        const val ACTION_START = "com.binod.safedns.START_VPN"
        const val ACTION_STOP = "com.binod.safedns.STOP_VPN"
        const val EXTRA_PRIMARY_DNS = "primary_dns"
        const val EXTRA_SECONDARY_DNS = "secondary_dns"
    }

    @Inject
    lateinit var blocklistPreferences: BlocklistPreferences

    @Inject
    lateinit var customProfilePreferences: CustomProfilePreferences

    @Inject
    lateinit var statsPreferences: StatsPreferences

    @Inject
    lateinit var whitelistPreferences: WhitelistPreferences

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var dnsResolver: DnsResolver
    private lateinit var packetHandler: PacketHandler

    private var primaryDns = "1.1.1.1"
    private var secondaryDns = "8.8.8.8"
    private var primaryDnsV6 = "2606:4700:4700::1111" // Cloudflare IPv6
    private var secondaryDnsV6 = "2001:4860:4860::8888" // Google IPv6

    private var blockedCount = 0
    private var allowedCount = 0
    private var notificationUpdateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VPN Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                primaryDns = intent.getStringExtra(EXTRA_PRIMARY_DNS) ?: "1.1.1.1"
                secondaryDns = intent.getStringExtra(EXTRA_SECONDARY_DNS) ?: "8.8.8.8"
                startVpn()
            }
            ACTION_STOP -> {
                stopVpn()
            }
        }

        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) {
            Log.d(TAG, "VPN already running")
            return
        }

        try {
            Log.d(TAG, "🔍 Stats preferences initialized: ${this::statsPreferences.isInitialized}")
            Log.d(TAG, "🔍 Whitelist preferences initialized: ${this::whitelistPreferences.isInitialized}")
            Log.d(TAG, "🔍 Custom profile preferences initialized: ${this::customProfilePreferences.isInitialized}")

            // Get domain counts for logging
            val blocklistDomains = blocklistPreferences.getBlockedDomains()
            val customDomains = customProfilePreferences.getCustomDomains()
            Log.d(TAG, "🔍 Blocklist size: ${blocklistDomains.size}")
            Log.d(TAG, "🔍 Custom domains size: ${customDomains.size}")
            Log.d(TAG, "🔍 Total blocked domains: ${blocklistDomains.size + customDomains.size}")

            dnsResolver = DnsResolver(primaryDns, secondaryDns)

            // UPDATED: Pass preferences instead of static domain set
            packetHandler = PacketHandler(
                blocklistPreferences = blocklistPreferences,
                customProfilePreferences = customProfilePreferences,
                whitelistPreferences = whitelistPreferences,
                statsPreferences = statsPreferences
            ) { blocked ->
                if (blocked) {
                    blockedCount++
                    Log.d(TAG, "📊 Blocked count: $blockedCount")
                } else {
                    allowedCount++
                    Log.d(TAG, "📊 Allowed count: $allowedCount")
                }
            }

            // Configure VPN with both IPv4 and IPv6 support
            val builder = Builder()
                .setSession("SafeDNS")
                // IPv4 configuration
                .addAddress(VPN_ADDRESS_V4, 32)
                .addRoute(VPN_ROUTE_V4, 0)
                .addDnsServer(primaryDns)
                .addDnsServer(secondaryDns)
                // IPv6 configuration
                .addAddress(VPN_ADDRESS_V6, 64)
                .addRoute(VPN_ROUTE_V6, 0)
                .addDnsServer(primaryDnsV6)
                .addDnsServer(secondaryDnsV6)
                // Common settings
                .setMtu(VPN_MTU)
                .setBlocking(false)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN")
                stopSelf()
                return
            }

            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification())

            // Start packet processing
            scope.launch {
                handlePackets()
            }

            // Start notification update job
            startNotificationUpdates()

            Log.d(TAG, "✅ VPN started successfully with IPv4 and IPv6 support")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN", e)
            stopVpn()
        }
    }

    private suspend fun handlePackets() = withContext(Dispatchers.IO) {
        val vpnInput = FileInputStream(vpnInterface!!.fileDescriptor)
        val vpnOutput = FileOutputStream(vpnInterface!!.fileDescriptor)

        val packet = ByteBuffer.allocate(VPN_MTU)
        var channel: DatagramChannel? = null

        try {
            channel = DatagramChannel.open()
            channel.connect(InetSocketAddress(primaryDns, 53))
            channel.configureBlocking(false)

            Log.d(TAG, "📡 Packet handling started")

            while (isRunning) {
                packet.clear()
                val length = vpnInput.channel.read(packet)

                if (length > 0) {
                    packet.flip()

                    // Process the packet (supports both IPv4 and IPv6)
                    val response = packetHandler.handlePacket(packet.array(), length, dnsResolver)

                    if (response != null) {
                        vpnOutput.write(response)
                    }
                }

                // Small delay to prevent CPU overuse
                Thread.sleep(1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling packets", e)
        } finally {
            channel?.close()
            vpnInput.close()
            vpnOutput.close()
        }
    }

    private fun startNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = scope.launch {
            while (isRunning) {
                delay(2000) // Update every 2 seconds
                updateNotification()
            }
        }
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN")
        isRunning = false

        try {
            notificationUpdateJob?.cancel()
            notificationUpdateJob = null

            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }

        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ad blocking and DNS filtering (IPv4 & IPv6)"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, AdBlockVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Get stats from preferences
        val totalBlocked = statsPreferences.getTotalBlocked()
        val totalAllowed = statsPreferences.getTotalAllowed()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeDNS Protection Active")
            .setContentText("Blocked: $totalBlocked | Allowed: $totalAllowed")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_stop,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification() {
        if (isRunning) {
            try {
                val notification = createNotification()
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        scope.cancel()
        Log.d(TAG, "VPN Service destroyed")
    }

    override fun onRevoke() {
        super.onRevoke()
        Log.d(TAG, "VPN revoked")
        stopVpn()
    }
}