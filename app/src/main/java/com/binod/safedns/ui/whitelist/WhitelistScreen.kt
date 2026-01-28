package com.binod.safedns.ui.whitelist

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    vm: WhitelistViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val domains by vm.domains.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val darkBlue1 = Color(0xFF1A3B4D)
    val darkBlue2 = Color(0xFF2C5F7A)
    val bg = Brush.verticalGradient(listOf(darkBlue1, darkBlue2, darkBlue1))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Whitelist", color = Color.White) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF4ECDC4)
            ) {
                Icon(Icons.Default.Add, "Add domain", tint = Color.White)
            }
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
                // Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4ECDC4).copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = Color(0xFF4ECDC4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "Whitelisted domains will never be blocked",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Domain List
                if (domains.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No whitelisted domains",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tap + to add a domain",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp
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
                        items(domains.toList(), key = { it }) { domain ->
                            WhitelistDomainItem(
                                domain = domain,
                                onDelete = { vm.removeDomain(domain) }
                            )
                        }

                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddWhitelistDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { domain ->
                vm.addDomain(domain)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun WhitelistDomainItem(
    domain: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2ECC71),
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.size(12.dp))

            Text(
                domain,
                fontSize = 15.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color(0xFFE74C3C),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddWhitelistDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var domain by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Domain to Whitelist") },
        text = {
            Column {
                Text(
                    "Enter a domain to allow (e.g., google.com)",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = domain,
                    onValueChange = {
                        domain = it.trim().lowercase()
                        error = null
                    },
                    label = { Text("Domain") },
                    placeholder = { Text("example.com") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4ECDC4),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF4ECDC4),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        cursorColor = Color(0xFF4ECDC4),
                        errorBorderColor = Color(0xFFE74C3C),
                        errorLabelColor = Color(0xFFE74C3C)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        domain.isBlank() -> error = "Domain cannot be empty"
                        !domain.contains(".") -> error = "Invalid domain format"
                        domain.startsWith(".") || domain.endsWith(".") ->
                            error = "Invalid domain format"
                        else -> onAdd(domain)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4ECDC4)
                )
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF1E3A4A)
    )
}