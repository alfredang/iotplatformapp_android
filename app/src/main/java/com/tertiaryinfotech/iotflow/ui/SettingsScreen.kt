package com.tertiaryinfotech.iotflow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.iotflow.SessionViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(session: SessionViewModel) {
    var showServer by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val version = remember {
        runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pi.versionName} (${pi.longVersionCode})"
        }.getOrDefault("1.0")
    }
    val user = session.user

    ScreenScaffold(title = "Settings") {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupCard("Account") {
                KeyValueRow("Name", user?.name ?: "—")
                HorizontalDivider()
                KeyValueRow("Email", user?.email ?: "—")
                user?.role?.let {
                    HorizontalDivider()
                    KeyValueRow("Role", it.replaceFirstChar { c -> c.uppercase() })
                }
            }

            GroupCard("Connection") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showServer = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Dns, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Server", style = MaterialTheme.typography.bodyLarge)
                }
            }

            GroupCard(null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { session.logout() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFE11D48))
                    Text("Sign out", color = Color(0xFFE11D48), style = MaterialTheme.typography.bodyLarge)
                }
            }

            GroupCard("Danger Zone") {
                if (!confirmDelete) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { confirmDelete = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.DeleteForever, null, tint = Color(0xFFE11D48))
                        Text("Delete Account", color = Color(0xFFE11D48), style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "This permanently deletes your account and removes your personal data. This cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        deleteError?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE11D48))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { confirmDelete = false; deleteError = null }, enabled = !deleting) {
                                Text("Cancel")
                            }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        deleting = true
                                        deleteError = null
                                        try {
                                            session.deleteAccount()
                                        } catch (e: Exception) {
                                            deleteError = e.localizedMessage ?: "Could not delete account."
                                        }
                                        deleting = false
                                    }
                                },
                                enabled = !deleting,
                            ) {
                                Text(
                                    if (deleting) "Deleting…" else "Permanently Delete Account",
                                    color = Color(0xFFE11D48),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            GroupCard(null) {
                KeyValueRow("Version", version)
            }
            Text(
                "IoTFlow — manage devices and monitor telemetry on the go.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    if (showServer) ServerSettingsDialog(onDismiss = { showServer = false })
}

@Composable
private fun GroupCard(title: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        title?.let {
            Text(
                it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) { content() }
        }
    }
}
