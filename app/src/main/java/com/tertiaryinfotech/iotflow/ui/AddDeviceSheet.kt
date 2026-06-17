package com.tertiaryinfotech.iotflow.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.iotflow.ApiClient
import com.tertiaryinfotech.iotflow.CreateDeviceResponse
import com.tertiaryinfotech.iotflow.DeviceProtocol
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceSheet(onAdded: () -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var proto by remember { mutableStateOf(DeviceProtocol.HTTP) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var created by remember { mutableStateOf<CreateDeviceResponse?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun add() {
        busy = true
        error = null
        scope.launch {
            try {
                created = ApiClient.createDevice(
                    name = name,
                    type = type,
                    location = location.ifEmpty { null },
                    proto = proto,
                )
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Could not add device."
            }
            busy = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val c = created
        if (c != null) {
            Text("Device Added", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF22C55E))
                Text("${c.device.name} is ready to connect.")
            }
            LabeledMono("Device ID", c.device.deviceId)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Device Token", style = MaterialTheme.typography.labelMedium)
                    Text(c.token, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { copyToClipboard(context, c.token) }) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Text("  Copy token")
                    }
                }
            }
            Text(
                "Save this token now — it is shown only once. Devices send telemetry to /api/telemetry using this token.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Button(onClick = onAdded, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        } else {
            Text("Add Device", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name (e.g. Living Room Sensor)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = type, onValueChange = { type = it },
                label = { Text("Type (e.g. Temperature)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("Location (optional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Text("Protocol", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceProtocol.entries.forEach { p ->
                    FilterChip(
                        selected = proto == p,
                        onClick = { proto = p },
                        label = { Text(p.label) },
                    )
                }
            }
            error?.let { Text(it, color = Color(0xFFE11D48), style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { add() },
                    enabled = name.isNotEmpty() && !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Add")
                }
            }
        }
    }
}

@Composable
private fun LabeledMono(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Device Token", text))
}
