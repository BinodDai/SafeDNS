@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.binod.safedns.ui.home

import ProfileSelector
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.binod.safedns.R
import com.binod.safedns.domain.model.ProtectionState

@Composable
fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit = {},
    onOpenCustomProfile: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenLogs: () -> Unit = {},
    onOpenWhitelist: () -> Unit = {}
) {
    val ui by vm.uiState.collectAsState()
    val vpnPermissionIntent by vm.vpnPermissionNeeded.collectAsState()

    val context = LocalContext.current

    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vm.onVpnPermissionGranted()
        } else {
            vm.onVpnPermissionDenied()
        }
    }

    // Handle VPN permission request
    LaunchedEffect(vpnPermissionIntent) {
        vpnPermissionIntent?.let { intent ->
            vpnPermissionLauncher.launch(intent)
        }
    }

    val darkBlue1 = Color(0xFF1A3B4D)
    val darkBlue2 = Color(0xFF2C5F7A)
    val darkBlue3 = Color(0xFF1A3B4D)

    val bg = Brush.verticalGradient(
        colors = listOf(darkBlue1, darkBlue2, darkBlue3)
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color(0xFF4ECDC4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->

        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status text at top
                Text(
                    text = when (ui.state) {
                        ProtectionState.ON -> stringResource(R.string.connected)
                        ProtectionState.CONNECTING -> stringResource(R.string.connecting)
                        ProtectionState.OFF -> stringResource(R.string.connect)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f)
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = stringResource(R.string.ip_address),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Blocked domains: ${ui.blockedDomains}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Spacer(Modifier.height(48.dp))

                // Protection Ring
                ProtectionRing(
                    state = ui.state,
                    duration = ui.durationText,
                    onClick = { vm.onToggleProtection() }
                )

                Spacer(Modifier.weight(1f))

                // Quick Access Cards
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessCard(
                        title = "Stats",
                        icon = Icons.Default.Assessment,
                        onClick = onOpenStats,
                        modifier = Modifier.weight(1f)
                    )
                    QuickAccessCard(
                        title = "Logs",
                        icon = Icons.Default.List,
                        onClick = onOpenLogs,
                        modifier = Modifier.weight(1f)
                    )
                    QuickAccessCard(
                        title = "Whitelist",
                        icon = Icons.Default.CheckCircle,
                        onClick = onOpenWhitelist,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                ProfileSelector(
                    profile = ui.profile,
                    upstreamLabel = ui.upstreamLabel,
                    state = ui.state,
                    onPickProfile = { vm.onSelectProfile(it) },
                    onOpenCustomProfile = onOpenCustomProfile
                )

                Spacer(Modifier.height(20.dp))

                // Auto-update toggle
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.auto_update_blocklists),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = ui.autoUpdateFilters,
                        onCheckedChange = { vm.onToggleAutoUpdate(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4ECDC4),
                            checkedTrackColor = Color(0xFF4ECDC4).copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF4ECDC4),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ProtectionRing(
    state: ProtectionState,
    duration: String,
    onClick: () -> Unit
) {
    val size = 280.dp
    val stroke = 8.dp
    val strokePx = with(LocalDensity.current) { stroke.toPx() }

    // Ring colors
    val inactiveRingColor = Color.White.copy(alpha = 0.15f)
    val activeRingColor = Color(0xFF4ECDC4)

    val progress = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == ProtectionState.ON) {
            progress.animateTo(1f, tween(900))
        } else {
            progress.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Background ring
            drawArc(
                color = inactiveRingColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active ring
            val sweep = when (state) {
                ProtectionState.ON -> 360f * progress.value
                ProtectionState.CONNECTING -> 270f
                ProtectionState.OFF -> 0f
            }

            if (sweep > 0f) {
                drawArc(
                    color = activeRingColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Center button
        ElevatedCard(
            onClick = onClick,
            shape = CircleShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFF2C5F7A).copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .size(size * 0.68f)
                .clip(CircleShape)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state == ProtectionState.ON) {
                        Text(
                            text = stringResource(R.string.duration),
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = duration,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Spacer(Modifier.height(12.dp))
                    }

                    // Power icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (state == ProtectionState.ON)
                                    Color(0xFF4ECDC4).copy(alpha = 0.2f)
                                else
                                    Color.White.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = stringResource(R.string.toggle),
                            tint = if (state == ProtectionState.ON)
                                Color(0xFF4ECDC4)
                            else
                                Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = when (state) {
                            ProtectionState.ON -> stringResource(R.string.tap_to_pause)
                            ProtectionState.CONNECTING -> stringResource(R.string.please_wait)
                            ProtectionState.OFF -> stringResource(R.string.tap_to_protect)
                        },
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}