package com.tertiaryinfotech.iotflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.iotflow.ApiClient
import com.tertiaryinfotech.iotflow.AlertRow
import com.tertiaryinfotech.iotflow.DashboardSummary
import com.tertiaryinfotech.iotflow.TelemetryRow
import com.tertiaryinfotech.iotflow.formatValue
import com.tertiaryinfotech.iotflow.relativeTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = summary == null
        error = null
        try {
            summary = ApiClient.dashboardSummary()
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Couldn't load dashboard."
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    ScreenScaffold(title = "Dashboard") {
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { scope.launch { load() } },
            modifier = Modifier.fillMaxSize(),
        ) {
            val s = summary
            when {
                s != null -> DashboardContent(s)
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> ErrorBanner(error!!) { scope.launch { load() } }
            }
        }
    }
}

@Composable
private fun DashboardContent(s: DashboardSummary) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard("Total Devices", "${s.counts.total}", Icons.Filled.Memory, Color(0xFF3B82F6), Modifier.weight(1f))
                StatCard("Online", "${s.counts.online}", Icons.Filled.Wifi, Color(0xFF22C55E), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard("Offline", "${s.counts.offline}", Icons.Filled.WifiOff, Color(0xFF9CA3AF), Modifier.weight(1f))
                StatCard("Active Alerts", "${s.counts.activeAlerts}", Icons.Filled.NotificationsActive, Color(0xFFF59E0B), Modifier.weight(1f))
            }
        }

        SectionCard("Latest Telemetry") {
            if (s.latestTelemetry.isEmpty()) {
                EmptyRow("No telemetry yet.")
            } else {
                s.latestTelemetry.forEachIndexed { i, t ->
                    TelemetryItem(t)
                    if (i < s.latestTelemetry.lastIndex) HorizontalDivider()
                }
            }
        }

        SectionCard("Recent Alerts") {
            if (s.recentAlerts.isEmpty()) {
                EmptyRow("No alerts. All good!")
            } else {
                s.recentAlerts.forEachIndexed { i, a ->
                    AlertItem(a)
                    if (i < s.recentAlerts.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TelemetryItem(t: TelemetryRow) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                t.device?.name ?: t.device?.deviceId ?: "Device",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                t.metric,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatValue(t.value),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
            Text(
                relativeTime(t.ts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun AlertItem(a: AlertRow) {
    val active = a.status == "ACTIVE"
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (active) Icons.Filled.Warning else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (active) Color(0xFFF59E0B) else Color(0xFF22C55E),
        )
        Column(Modifier.weight(1f)) {
            Text(
                a.device?.name ?: "Device",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                a.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Text(
            relativeTime(a.triggeredAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
fun EmptyRow(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}
