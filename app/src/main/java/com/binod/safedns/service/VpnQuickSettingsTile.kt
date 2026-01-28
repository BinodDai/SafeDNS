package com.binod.safedns.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.binod.safedns.R
import com.binod.safedns.data.local.preferences.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class VpnQuickSettingsTile : TileService() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private var isVpnActive = false

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onClick() {
        super.onClick()

        if (isVpnActive) {
            // Stop VPN
            val intent = Intent(this, AdBlockVpnService::class.java).apply {
                action = AdBlockVpnService.ACTION_STOP
            }
            startService(intent)
            isVpnActive = false
        } else {
            // Start VPN
            val profile = preferencesManager.getSelectedProfile()
            val (primary, secondary) = profile.getUpstreamDns()

            val intent = Intent(this, AdBlockVpnService::class.java).apply {
                action = AdBlockVpnService.ACTION_START
                putExtra(AdBlockVpnService.EXTRA_PRIMARY_DNS, primary)
                putExtra(AdBlockVpnService.EXTRA_SECONDARY_DNS, secondary)
            }
            startService(intent)
            isVpnActive = true
        }

        updateTileState()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateTileState() {
        qsTile?.apply {
            state = if (isVpnActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "SafeDNS"
            subtitle = if (isVpnActive) "Protected" else "Not Protected"

            // Set icon
            icon = Icon.createWithResource(
                this@VpnQuickSettingsTile,
                if (isVpnActive) R.drawable.ic_shield else R.drawable.ic_shield
            )

            updateTile()
        }
    }
}