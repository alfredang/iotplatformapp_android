package com.tertiaryinfotech.iotflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.iotflow.ApiClient
import com.tertiaryinfotech.iotflow.Automation
import com.tertiaryinfotech.iotflow.relativeTime
import kotlinx.coroutines.launch

/**
 * n8n Automations — lists the project's automation flows and lets the user
 * trigger one (sends a sample event to its n8n webhook, same as the web Test
 * button) or enable/disable it. Mirrors the iOS Automations tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationsScreen() {
    var automations by remember { mutableStateOf<List<Automation>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    suspend fun load() {
        loading = automations == null
        error = null
        try {
            automations = ApiClient.automations()
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Couldn't load automations."
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    ScreenScaffold(title = "Automations") {
        Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { scope.launch { load() } },
            modifier = Modifier.fillMaxSize(),
        ) {
            val list = automations
            when {
                list != null -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (list.isEmpty()) {
                        EmptyRow("No automations yet. Create n8n flows on the web dashboard.")
                    } else {
                        list.forEach { a ->
                            AutomationCard(
                                automation = a,
                                onTrigger = {
                                    scope.launch {
                                        val msg = try {
                                            val r = ApiClient.triggerAutomation(a.id)
                                            "▶ Triggered ${a.name}" + (r.status?.let { " ($it)" } ?: "")
                                        } catch (e: Exception) {
                                            "Flow error: ${e.localizedMessage ?: "failed"}"
                                        }
                                        snackbar.showSnackbar(msg)
                                    }
                                },
                                onToggle = { enabled ->
                                    scope.launch {
                                        runCatching { ApiClient.setAutomationEnabled(a.id, enabled) }
                                            .onFailure { snackbar.showSnackbar("Could not update ${a.name}.") }
                                        load()
                                    }
                                },
                            )
                        }
                    }
                    Text(
                        "Trigger sends a sample event to the flow's n8n webhook — the same as the web Test button.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> ErrorBanner(error!!) { scope.launch { load() } }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun AutomationCard(
    automation: Automation,
    onTrigger: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color(0xFFF59E0B))
                Text(
                    automation.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = automation.enabled, onCheckedChange = onToggle)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip(automation.eventLabel, MaterialTheme.colorScheme.primary)
                automation.metric?.let { TagChip(it, Color(0xFF8B5CF6)) }
                TagChip(
                    if (automation.enabled) "Enabled" else "Disabled",
                    if (automation.enabled) Color(0xFF22C55E) else Color(0xFF9CA3AF),
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when (automation.lastOk) {
                        true -> Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                        false -> Icon(Icons.Filled.ErrorOutline, null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                        null -> {}
                    }
                    Text(
                        automation.lastFiredAt?.let { "Last fired ${relativeTime(it)}" } ?: "Never fired",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                OutlinedButton(onClick = onTrigger) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Text("Trigger flow", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
