package com.binod.safedns.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
 fun DnsConfigDialog(
    currentPrimary: String,
    currentSecondary: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var primary by remember { mutableStateOf(currentPrimary) }
    var secondary by remember { mutableStateOf(currentSecondary) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure DNS Servers") },
        text = {
            Column {
                OutlinedTextField(
                    value = primary,
                    onValueChange = { primary = it },
                    label = { Text("Primary DNS") },
                    placeholder = { Text("1.1.1.1") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = secondary,
                    onValueChange = { secondary = it },
                    label = { Text("Secondary DNS") },
                    placeholder = { Text("8.8.8.8") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(primary, secondary) },
                enabled = primary.isNotBlank() && secondary.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}