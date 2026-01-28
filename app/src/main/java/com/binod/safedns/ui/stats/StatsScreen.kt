package com.binod.safedns.ui.stats

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    vm: StatsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val stats by vm.stats.collectAsState()

    LaunchedEffect(Unit) {
        vm.refresh()
    }

    val darkBlue1 = Color(0xFF1A3B4D)
    val darkBlue2 = Color(0xFF2C5F7A)
    val bg = Brush.verticalGradient(listOf(darkBlue1, darkBlue2, darkBlue1))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { vm.resetStats() }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Reset",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A3B4D)
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
        ) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview Cards
                item {
                    Text(
                        "Overview",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Blocked",
                            value = formatNumber(stats.totalBlocked),
                            subtitle = "Today: ${stats.todayBlocked}",
                            icon = Icons.Default.Block,
                            color = Color(0xFFE74C3C),
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Allowed",
                            value = formatNumber(stats.totalAllowed),
                            subtitle = "Today: ${stats.todayAllowed}",
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF2ECC71),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Block Rate",
                            value = "${stats.blockPercentage.toInt()}%",
                            subtitle = "${stats.totalQueries} total",
                            icon = Icons.Default.Speed,
                            color = Color(0xFF4ECDC4),
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Data Saved",
                            value = formatBytes(stats.dataSaved),
                            subtitle = "Estimated",
                            icon = Icons.Default.Speed,
                            color = Color(0xFFF39C12),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Block Rate Progress
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Protection Effectiveness",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    "${stats.blockPercentage.toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4ECDC4)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = stats.blockPercentage / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = Color(0xFF4ECDC4),
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${stats.totalBlocked} blocked",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    "${stats.totalAllowed} allowed",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Top Blocked Domains
                if (stats.topBlockedDomains.isNotEmpty()) {
                    item {
                        Text(
                            "Top Blocked Domains",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(stats.topBlockedDomains) { domain ->
                        TopBlockedDomainItem(
                            domain = domain.domain,
                            count = domain.count,
                            maxCount = stats.topBlockedDomains.firstOrNull()?.count ?: 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        title,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        value,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun TopBlockedDomainItem(
    domain: String,
    count: Int,
    maxCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    domain,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    count.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE74C3C)
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = count.toFloat() / maxCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFFE74C3C),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 1_000_000 -> "${num / 1_000_000}.${(num % 1_000_000) / 100_000}M"
        num >= 1_000 -> "${num / 1_000}.${(num % 1_000) / 100}K"
        else -> num.toString()
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "${bytes / 1_000_000_000}.${(bytes % 1_000_000_000) / 100_000_000}GB"
        bytes >= 1_000_000 -> "${bytes / 1_000_000}.${(bytes % 1_000_000) / 100_000}MB"
        bytes >= 1_000 -> "${bytes / 1_000}.${(bytes % 1_000) / 100}KB"
        else -> "${bytes}B"
    }
}