package com.tertiaryinfotech.iotflow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

// MARK: - API errors

@Serializable
data class APIErrorResponse(val error: String? = null)

class ApiException(message: String) : Exception(message)
