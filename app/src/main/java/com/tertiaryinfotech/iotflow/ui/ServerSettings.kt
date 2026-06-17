package com.tertiaryinfotech.iotflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.iotflow.AppConfig
import com.tertiaryinfotech.iotflow.Store

@Composable
fun ServerSettingsDialog(onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(Store.serverURL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("https://iot.example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Point the app at your self-hosted IoTFlow instance. Defaults to the public platform.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var v = draft.trim()
                if (v.endsWith("/")) v = v.dropLast(1)
                if (v.isNotEmpty()) Store.serverURL = v
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                Store.serverURL = AppConfig.DEFAULT_SERVER
                onDismiss()
            }) { Text("Reset to default") }
        },
    )
}
