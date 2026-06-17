package com.tertiaryinfotech.iotflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.tertiaryinfotech.iotflow.ApiClient
import com.tertiaryinfotech.iotflow.Device
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen() {
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Device?>(null) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    suspend fun load() {
        loading = true
        error = null
        try {
            devices = ApiClient.devices()
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Couldn't load devices."
        }
        loading = false
    }

    fun delete(device: Device) {
        devices = devices.filterNot { it.id == device.id }
        scope.launch { runCatching { ApiClient.deleteDevice(device.id) } }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Devices") },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add device")
                    }
                },
            )
        },
        floatingActionButton = {
            if (devices.isNotEmpty()) {
                FloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add device")
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { scope.launch { load() } },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                devices.isEmpty() && loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                devices.isEmpty() && error != null ->
                    ErrorBanner(error!!) { scope.launch { load() } }
                devices.isEmpty() ->
                    EmptyDevices { showAdd = true }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(devices, key = { it.id }) { device ->
                        DeviceRow(
                            device = device,
                            onClick = { detail = device },
                            onDelete = { delete(device) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }, sheetState = sheetState) {
            AddDeviceSheet(
                onAdded = { showAdd = false; scope.launch { load() } },
                onCancel = { showAdd = false },
            )
        }
    }

    detail?.let { d ->
        ModalBottomSheet(onDismissRequest = { detail = null }) {
            DeviceDetailSheet(d)
        }
    }
}

@Composable
private fun EmptyDevices(onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Memory, null, modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Spacer(Modifier.size(12.dp))
        Text("No Devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(6.dp))
        Text(
            "Add your first device to start streaming telemetry.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.size(16.dp))
        PrimaryButtonCompact("Add Device", onAdd)
    }
}

@Composable
private fun PrimaryButtonCompact(text: String, onClick: () -> Unit) {
    androidx.compose.material3.Button(onClick = onClick) { Text(text) }
}

@Composable
private fun DeviceRow(device: Device, onClick: () -> Unit, onDelete: () -> Unit) {
    val online = device.isOnline
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(
                    (if (online) Color(0xFF22C55E) else Color(0xFF9CA3AF)).copy(alpha = 0.15f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Memory, null,
                tint = if (online) Color(0xFF22C55E) else Color(0xFF9CA3AF),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "${device.proto ?: "HTTP"} · ${device.deviceId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
            )
        }
        StatusPill(online)
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}
