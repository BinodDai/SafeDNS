package com.binod.safedns.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.binod.safedns.ui.components.AddBlocklistDialog
import com.binod.safedns.ui.components.BlocklistItem
import com.binod.safedns.ui.components.DnsConfigCard
import com.binod.safedns.ui.components.DnsConfigDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProfileScreen(
    vm: CustomProfileViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by vm.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showDnsDialog by remember { mutableStateOf(false) }

    val darkBlue1 = Color(0xFF1A3B4D)
    val darkBlue2 = Color(0xFF2C5F7A)
    val bg = Brush.verticalGradient(listOf(darkBlue1, darkBlue2, darkBlue1))

    val displayUrls = vm.getDisplayUrls()
    val totalUrls = uiState.blocklistUrls.size
    val hasMore = totalUrls > 3 && !uiState.showAllUrls

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
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
                    .padding(16.dp)
            ) {
                // DNS Configuration Section
                item {
                    Text(
                        "DNS Servers",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                item {
                    DnsConfigCard(
                        primaryDns = uiState.primaryDns,
                        secondaryDns = uiState.secondaryDns,
                        onClick = { showDnsDialog = true }
                    )
                }

                // Blocklist URLs Section
                item {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Blocklist Sources ($totalUrls)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFF4ECDC4))
                        }
                    }
                }

                // Search bar (show if > 9 items)
                if (totalUrls > 7) {
                    item {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { vm.updateSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search blocklists...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4ECDC4),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (uiState.blocklistUrls.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "No blocklists added yet.\nTap + to add sources.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(displayUrls) { url ->
                        BlocklistItem(
                            url = url,
                            onDelete = { vm.removeBlocklistUrl(url) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // "View All" button (show if > 3 items and not showing all)
                    if (hasMore) {
                        item {
                            Button(
                                onClick = { vm.toggleShowAllUrls() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "View All (${totalUrls - 3} more)",
                                    color = Color(0xFF4ECDC4),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // "Show Less" button (if showing all and > 3 items)
                    if (uiState.showAllUrls && totalUrls > 3) {
                        item {
                            Button(
                                onClick = { vm.toggleShowAllUrls() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Show Less",
                                    color = Color(0xFF4ECDC4),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Custom Domains Section
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Custom Blocked Domains",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Text(
                        "Add domains to block manually (one per line)",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.customDomains,
                        onValueChange = { vm.updateCustomDomains(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = {
                            Text("example.com\nads.example.com\ntracker.net")
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4ECDC4),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }

                // Save Button
                item {
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { vm.saveConfiguration(); onBack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4ECDC4)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Save Configuration",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddBlocklistDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { url ->
                vm.addBlocklistUrl(url)
                showAddDialog = false
            }
        )
    }

    if (showDnsDialog) {
        DnsConfigDialog(
            currentPrimary = uiState.primaryDns,
            currentSecondary = uiState.secondaryDns,
            onDismiss = { showDnsDialog = false },
            onSave = { primary, secondary ->
                vm.updateDnsServers(primary, secondary)
                showDnsDialog = false
            }
        )
    }
}