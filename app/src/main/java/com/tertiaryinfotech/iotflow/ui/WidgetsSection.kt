package com.tertiaryinfotech.iotflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.iotflow.ApiClient
import com.tertiaryinfotech.iotflow.DashWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "My Widgets" section on the Dashboard — mirrors the web dashboard: display
 * widgets (number, gauge, LED, status, chart-latest) plus interactive control
 * widgets (switch, slider, button) that trigger device actions via the command
 * API. Rendered as a 2-column grid.
 */
@Composable
fun DashboardWidgetsSection() {
    var widgets by remember { mutableStateOf<List<DashWidget>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        widgets = runCatching { ApiClient.dashboardWidgets() }.getOrDefault(emptyList())
        loaded = true
    }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("My Widgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (loaded && widgets.isEmpty()) {
            EmptyRow("No widgets yet. Add them on the web dashboard.")
        } else {
            // LINE/BAR/TERMINAL span the full row (like web/iOS); the rest sit in pairs.
            val wideTypes = setOf("LINE", "BAR", "TERMINAL", "ALERTS", "MAP")
            val rows = buildList {
                var pair = mutableListOf<DashWidget>()
                widgets.forEach { w ->
                    if (w.type in wideTypes) {
                        if (pair.isNotEmpty()) { add(pair.toList()); pair = mutableListOf() }
                        add(listOf(w))
                    } else {
                        pair.add(w)
                        if (pair.size == 2) { add(pair.toList()); pair = mutableListOf() }
                    }
                }
                if (pair.isNotEmpty()) add(pair.toList())
            }
            rows.forEach { row ->
                if (row.size == 1 && row[0].type in wideTypes) {
                    WidgetCard(row[0], Modifier.fillMaxWidth())
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { w -> WidgetCard(w, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(w: DashWidget, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(16.dp).heightIn(min = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                w.title.ifEmpty { w.device?.name ?: w.type },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            when (w.type) {
                "NUMBER" -> w.device?.let { d -> w.metric?.let { LatestNumber(d.id, it) } }
                "LINE", "BAR" -> w.device?.let { d -> w.metric?.let { ChartWidget(d.id, it, bars = w.type == "BAR") } }
                "GAUGE" -> w.device?.let { d -> w.metric?.let { GaugeWidget(d.id, it, w.config?.min ?: 0.0, w.config?.max ?: 100.0) } }
                "LED" -> w.device?.let { d -> w.metric?.let { LedWidget(d.id, it) } }
                "STATUS" -> w.device?.let { StatusPill(it.isOnline) }
                "SWITCH" -> w.device?.let { SwitchWidget(it.id, w.pin) }
                "SLIDER" -> w.device?.let { SliderWidget(it.id, w.pin, w.config?.min ?: 0.0, w.config?.max ?: 100.0) }
                "BUTTON" -> w.device?.let { ButtonWidget(it.id, w.pin) }
                "TERMINAL" -> w.device?.let { TerminalWidget(it.id, w.pin) }
                else -> Text(w.type, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun fmt(v: Double?): String {
    if (v == null) return "—"
    return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
}

// MARK: - Display widgets

@Composable
private fun LatestNumber(deviceId: String, metric: String, caption: String? = null) {
    var value by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(deviceId, metric) {
        while (true) {
            value = runCatching { ApiClient.latestValue(deviceId, metric) }.getOrNull()
            delay(5000)
        }
    }
    Column {
        Text(fmt(value), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            caption ?: metric,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun GaugeWidget(deviceId: String, metric: String, min: Double, max: Double) {
    var value by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(deviceId, metric) {
        while (true) {
            value = runCatching { ApiClient.latestValue(deviceId, metric) }.getOrNull()
            delay(5000)
        }
    }
    val frac = value?.let { ((it - min) / (max - min)).coerceIn(0.0, 1.0) } ?: 0.0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(fmt(value), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(progress = { frac.toFloat() }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(min), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(fmt(max), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun ChartWidget(deviceId: String, metric: String, bars: Boolean) {
    var points by remember { mutableStateOf<List<Double>>(emptyList()) }
    LaunchedEffect(deviceId, metric) {
        while (true) {
            runCatching { ApiClient.telemetryHistory(deviceId, metric) }
                .getOrNull()
                ?.let { points = it.mapNotNull { p -> p.value } }
            delay(10_000)
        }
    }
    val color = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(fmt(points.lastOrNull()), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Canvas(Modifier.fillMaxWidth().height(56.dp)) {
            if (points.size < 2) return@Canvas
            val min = points.min()
            val max = points.max()
            val range = (max - min).takeIf { it > 1e-9 } ?: 1.0
            fun y(v: Double) = size.height * (1f - ((v - min) / range).toFloat()) * 0.94f + size.height * 0.03f
            if (bars) {
                val slot = size.width / points.size
                val barWidth = slot * 0.6f
                points.forEachIndexed { i, v ->
                    val x = slot * (i + 0.5f)
                    drawRect(
                        color,
                        topLeft = Offset(x - barWidth / 2f, y(v)),
                        size = Size(barWidth, size.height - y(v)),
                    )
                }
            } else {
                val stepX = size.width / (points.size - 1)
                val path = Path()
                points.forEachIndexed { i, v ->
                    if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * stepX, y(v))
                }
                drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Text(
            metric,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun LedWidget(deviceId: String, metric: String) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(deviceId, metric) {
        while (true) {
            on = (runCatching { ApiClient.latestValue(deviceId, metric) }.getOrNull() ?: 0.0) > 0.0
            delay(5000)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).background(if (on) Color(0xFF22C55E) else Color(0xFF9CA3AF), CircleShape))
        Text(if (on) "ON" else "OFF", style = MaterialTheme.typography.titleMedium)
    }
}

// MARK: - Control widgets (trigger actions)

@Composable
private fun SwitchWidget(deviceId: String, pin: String) {
    var isOn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(deviceId, pin) {
        isOn = (runCatching { ApiClient.pinStates(deviceId) }.getOrNull()?.get(pin) ?: 0.0) == 1.0
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Pin $pin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Switch(checked = isOn, onCheckedChange = { v ->
            isOn = v
            scope.launch { runCatching { ApiClient.setCommand(deviceId, pin, if (v) 1.0 else 0.0) } }
        })
    }
}

@Composable
private fun SliderWidget(deviceId: String, pin: String, min: Double, max: Double) {
    var value by remember { mutableStateOf(min.toFloat()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(deviceId, pin) {
        val v = runCatching { ApiClient.pinStates(deviceId) }.getOrNull()?.get(pin) ?: min
        value = v.coerceIn(min, max).toFloat()
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Pin $pin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(fmt(value.toDouble()), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            valueRange = min.toFloat()..max.toFloat(),
            onValueChangeFinished = {
                scope.launch { runCatching { ApiClient.setCommand(deviceId, pin, value.toDouble()) } }
            },
        )
    }
}

@Composable
private fun ButtonWidget(deviceId: String, pin: String) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Button(
        onClick = {
            scope.launch {
                busy = true
                runCatching { ApiClient.setCommand(deviceId, pin, 1.0) }
                delay(400)
                runCatching { ApiClient.setCommand(deviceId, pin, 0.0) }
                busy = false
            }
        },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (busy) "Sending…" else "Press") }
}

@Composable
private fun TerminalWidget(deviceId: String, pin: String) {
    var text by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Send to $pin…") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(
                onClick = {
                    val msg = text
                    if (msg.isNotEmpty()) {
                        scope.launch { runCatching { ApiClient.setCommand(deviceId, pin, null, msg) } }
                        sent = (listOf(msg) + sent).take(4)
                        text = ""
                    }
                },
                enabled = text.isNotEmpty(),
            ) { Text("Send") }
        }
        sent.forEach {
            Text("→ $it", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
