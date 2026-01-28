package com.binod.safedns.ui.logs

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.binod.safedns.domain.model.DnsQuery
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogFilter {
    ALL, BLOCKED, ALLOWED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryLogsScreen(
    vm: QueryLogsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val queries by vm.queries.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LogFilter.ALL) }

    val darkBlue1 = Color(0xFF1A3B4D)
    val darkBlue2 = Color(0xFF2C5F7A)
    val bg = Brush.verticalGradient(listOf(darkBlue1, darkBlue2, darkBlue1))

    // Filter queries
    val filteredQueries = queries.filter { query ->
        val matchesSearch = searchQuery.isBlank() ||
                query.domain.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filter) {
            LogFilter.ALL -> true
            LogFilter.BLOCKED -> query.isBlocked
            LogFilter.ALLOWED -> !query.isBlocked
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Query Logs", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { vm.clearLogs() }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Clear", color = Color.White.copy(alpha = 0.7f))
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
            Column(Modifier.fillMaxSize()) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search domains...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = Color.White.copy(0.6f))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, tint = Color.White.copy(0.6f))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4ECDC4),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = Color(0xFF4ECDC4)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Filter Chips
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filter == LogFilter.ALL,
                        onClick = { filter = LogFilter.ALL },
                        label = { Text("All (${queries.size})") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.FilterList,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4ECDC4),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = filter == LogFilter.BLOCKED,
                        onClick = { filter = LogFilter.BLOCKED },
                        label = { Text("Blocked (${queries.count { it.isBlocked }})") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Block,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE74C3C),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = filter == LogFilter.ALLOWED,
                        onClick = { filter = LogFilter.ALLOWED },
                        label = { Text("Allowed (${queries.count { !it.isBlocked }})") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2ECC71),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Query List
                if (filteredQueries.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchQuery.isBlank()) "No queries yet" else "No results found",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredQueries, key = { it.id }) { query ->
                            QueryLogItem(query, onWhitelist = { vm.addToWhitelist(query.domain) })
                        }

                        item {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueryLogItem(
    query: DnsQuery,
    onWhitelist: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (query.isBlocked)
                Color(0xFFE74C3C).copy(alpha = 0.1f)
            else
                Color(0xFF2ECC71).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (query.isBlocked)
                            Color(0xFFE74C3C).copy(alpha = 0.2f)
                        else
                            Color(0xFF2ECC71).copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (query.isBlocked) Icons.Default.Block else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (query.isBlocked) Color(0xFFE74C3C) else Color(0xFF2ECC71),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Domain and Time
            Column(Modifier.weight(1f)) {
                Text(
                    query.domain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatTime(query.timestamp),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // Whitelist button for blocked domains
            if (query.isBlocked) {
                TextButton(onClick = onWhitelist) {
                    Text(
                        "Whitelist",
                        fontSize = 11.sp,
                        color = Color(0xFF4ECDC4)
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}