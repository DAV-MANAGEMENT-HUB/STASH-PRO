package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sync.SyncStatus
import com.example.ui.theme.EmeraldGreen

@Composable
fun CloudSyncDialog(
    onDismiss: () -> Unit,
    syncStatus: SyncStatus,
    userEmail: String?,
    isLoggedIn: Boolean,
    onSignIn: (String) -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit
) {
    var emailInput by remember { mutableStateOf(userEmail ?: "") }
    var endpointInput by remember { mutableStateOf("https://stash-api.vercel.app") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Cloud & Vercel Sync", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "STASH operates local-first. You can connect your vault to a Vercel-hosted API and secure database for cross-device synchronization and backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = endpointInput,
                    onValueChange = { endpointInput = it },
                    label = { Text("Vercel / Cloud API Endpoint") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cloud_endpoint_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoggedIn) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Signed in as", style = MaterialTheme.typography.labelSmall)
                                Text(userEmail ?: "", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            TextButton(onClick = onSignOut) {
                                Text("Sign Out")
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Your Email Account") },
                        placeholder = { Text("user@example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("user_email_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { if (emailInput.isNotBlank()) onSignIn(emailInput.trim()) },
                        enabled = emailInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("sign_in_button")
                    ) {
                        Text("Connect Account")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sync Status Indicator
                when (syncStatus) {
                    is SyncStatus.Idle -> {
                        Text(
                            text = "Vault state: Local storage active & encrypted.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    is SyncStatus.Syncing -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synchronizing with Vercel API...", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    is SyncStatus.Success -> {
                        Text(
                            text = "✓ ${syncStatus.message}",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                        )
                    }
                    is SyncStatus.Error -> {
                        Text(
                            text = "Offline note: ${syncStatus.error}. All data safe locally.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zero-telemetry. Client secrets never hardcoded.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSyncNow,
                modifier = Modifier.testTag("sync_now_button")
            ) {
                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sync Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
