package com.tertiaryinfotech.iotflow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

// MARK: - Auth

@Serializable
data class CSRFResponse(val csrfToken: String)

@Serializable
data class CallbackResponse(val url: String? = null)

@Serializable
data class SessionResponse(val user: SessionUser? = null)

@Serializable
data class SessionUser(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
)

// MARK: - Devices

enum class DeviceProtocol(val label: String) {
    HTTP("HTTP REST"),
    MQTT("MQTT"),
    WEBSOCKET("WebSocket");
}

@Serializable
data class Device(
    val id: String,
    val name: String,
    val type: String? = null,
    val deviceId: String,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("protocol") val proto: String? = null,
    val status: String = "OFFLINE",
    val lastSeen: String? = null,
    val createdAt: String? = null,
    @SerialName("_count") val count: DeviceCount? = null,
) {
    val isOnline: Boolean get() = status == "ONLINE"
    val telemetryCount: Int get() = count?.telemetry ?: 0
}

@Serializable
data class DeviceCount(val telemetry: Int? = null)

@Serializable
data class DevicesResponse(
    val devices: List<Device> = emptyList(),
    val projectId: String? = null,
)

@Serializable
data class CreateDeviceResponse(val device: Device, val token: String)

// MARK: - Dashboard

@Serializable
data class DashboardSummary(
    val counts: Counts,
    val latestTelemetry: List<TelemetryRow> = emptyList(),
    val recentAlerts: List<AlertRow> = emptyList(),
    val devices: List<DeviceLite> = emptyList(),
)

@Serializable
data class Counts(
    val total: Int = 0,
    val online: Int = 0,
    val offline: Int = 0,
    val activeAlerts: Int = 0,
)

@Serializable
data class DeviceLite(
    val id: String,
    val name: String,
    val deviceId: String,
    val status: String = "OFFLINE",
    val lastSeen: String? = null,
)

@Serializable
data class TelemetryRow(
    val id: String,
    val ts: String? = null,
    val metric: String,
    val value: Double? = null,
    val device: TelemetryDevice? = null,
) {
    @Serializable
    data class TelemetryDevice(val name: String? = null, val deviceId: String? = null)
}

@Serializable
data class AlertRow(
    val id: String,
    val message: String,
    val value: Double? = null,
    val status: String = "ACTIVE",
    val triggeredAt: String? = null,
    val device: AlertDevice? = null,
) {
    @Serializable
    data class AlertDevice(val name: String? = null)
}

// MARK: - Dashboard widgets + device control (virtual pins)

@Serializable
data class WidgetsResponse(val widgets: List<DashWidget> = emptyList())

@Serializable
data class DashWidget(
    val id: String,
    val type: String,
    val title: String = "",
    val metric: String? = null,
    val config: WidgetConfig? = null,
    val device: WidgetDevice? = null,
) {
    /** Virtual pin a control widget writes to (config.pin, default "V1"). */
    val pin: String get() = config?.pin ?: "V1"
    val isControl: Boolean get() = type in listOf("BUTTON", "SWITCH", "SLIDER", "TERMINAL")
}

@Serializable
data class WidgetConfig(
    val min: Double? = null,
    val max: Double? = null,
    val pin: String? = null,
)

@Serializable
data class WidgetDevice(
    val id: String,
    val name: String,
    val deviceId: String,
    val status: String = "OFFLINE",
) {
    val isOnline: Boolean get() = status == "ONLINE"
}

@Serializable
data class TelemetryLatestResponse(val telemetry: List<TelemetryValue> = emptyList()) {
    @Serializable
    data class TelemetryValue(val value: Double? = null)
}

/** GET /api/devices/:id/command → { state: { "V1": 1, "msg": "hi" } }. */
@Serializable
data class PinStateResponse(val state: Map<String, JsonElement> = emptyMap()) {
    /** Numeric value for a pin, or null if absent / non-numeric. */
    fun number(pin: String): Double? =
        state[pin]?.let { runCatching { it.jsonPrimitive.doubleOrNull }.getOrNull() }
}

/** GET /api/devices/:id/telemetry → { telemetry: [...] } — recent history for charts. */
@Serializable
data class TelemetryHistoryResponse(val telemetry: List<TelemetryPoint> = emptyList()) {
    @Serializable
    data class TelemetryPoint(val ts: String? = null, val value: Double? = null)
}

// MARK: - Automations (n8n)

@Serializable
data class AutomationsResponse(val automations: List<Automation> = emptyList())

@Serializable
data class Automation(
    val id: String,
    val name: String,
    val event: String,
    val metric: String? = null,
    val n8nWebhookUrl: String = "",
    val enabled: Boolean = true,
    val lastFiredAt: String? = null,
    val lastStatus: String? = null,
) {
    val eventLabel: String get() = when (event) {
        "TELEMETRY" -> "Telemetry"
        "ALERT" -> "Alert"
        "DEVICE_ONLINE" -> "Device online"
        "DEVICE_OFFLINE" -> "Device offline"
        "COMMAND" -> "Command"
        else -> event
    }

    /** True/false when the last n8n webhook call is known to have succeeded/failed. */
    val lastOk: Boolean? get() = lastStatus?.let {
        it.startsWith("2") || it.equals("ok", ignoreCase = true)
    }
}

@Serializable
data class AutomationActionResponse(val ok: Boolean? = null, val status: String? = null)

// MARK: - API errors

@Serializable
data class APIErrorResponse(val error: String? = null)

class ApiException(message: String) : Exception(message)
