package com.tertiaryinfotech.iotflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.iotflow.Device
import com.tertiaryinfotech.iotflow.relativeTime
import com.tertiaryinfotech.iotflow.shortDate

@Composable
fun DeviceDetailSheet(device: Device) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(device.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Status", style = MaterialTheme.typography.bodyMedium)
            StatusPill(device.isOnline)
        }
        HorizontalDivider()
        KeyValueRow("Device ID", device.deviceId, mono = true)
        HorizontalDivider()
        KeyValueRow("Protocol", device.proto ?: "HTTP")
        device.type?.let { HorizontalDivider(); KeyValueRow("Type", it) }
        device.location?.takeIf { it.isNotEmpty() }?.let { HorizontalDivider(); KeyValueRow("Location", it) }
        HorizontalDivider()
        KeyValueRow("Telemetry points", "${device.telemetryCount}")
        device.lastSeen?.let { HorizontalDivider(); KeyValueRow("Last seen", relativeTime(it)) }
        device.createdAt?.let { HorizontalDivider(); KeyValueRow("Added", shortDate(it)) }
        if (device.latitude != null && device.longitude != null) {
            HorizontalDivider()
            KeyValueRow("Latitude", String.format("%.5f", device.latitude))
            HorizontalDivider()
            KeyValueRow("Longitude", String.format("%.5f", device.longitude))
        }
    }
}
