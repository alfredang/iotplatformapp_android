package com.tertiaryinfotech.iotflow

import java.time.Instant
import java.util.UUID

/**
 * Canned data so the app's full UI can be explored without a backend — used for
 * the "Explore demo" entry point and for Play Store review.
 */
object DemoData {

    val user = SessionUser(id = "demo", name = "Demo User", email = "demo@iotflow.app", role = "ADMIN")

    private fun ago(seconds: Long): String = Instant.now().minusSeconds(seconds).toString()

    fun devices(): List<Device> = listOf(
        device("d1", "Living Room Sensor", "Temperature", "lr-sensor", "HTTP", "ONLINE", "Home", 842, ago(30)),
        device("d2", "Warehouse Gateway", "Gateway", "wh-gw", "MQTT", "ONLINE", "Depot A", 15203, ago(95)),
        device("d3", "Rooftop Weather", "Weather", "roof-wx", "MQTT", "ONLINE", "Building 3", 6410, ago(160)),
        device("d4", "Cold Storage", "Temperature", "cold-1", "HTTP", "OFFLINE", "Depot A", 320, ago(7200)),
    )

    fun summary(): DashboardSummary = DashboardSummary(
        counts = Counts(total = 4, online = 3, offline = 1, activeAlerts = 1),
        latestTelemetry = listOf(
            TelemetryRow("t1", ago(30), "temperature", 22.4, TelemetryRow.TelemetryDevice("Living Room Sensor", "lr-sensor")),
            TelemetryRow("t2", ago(30), "humidity", 48.0, TelemetryRow.TelemetryDevice("Living Room Sensor", "lr-sensor")),
            TelemetryRow("t3", ago(95), "power_kw", 3.7, TelemetryRow.TelemetryDevice("Warehouse Gateway", "wh-gw")),
            TelemetryRow("t4", ago(160), "wind_ms", 5.1, TelemetryRow.TelemetryDevice("Rooftop Weather", "roof-wx")),
        ),
        recentAlerts = listOf(
            AlertRow("a1", "Temperature above 8°C threshold", null, "ACTIVE", ago(600), AlertRow.AlertDevice("Cold Storage")),
            AlertRow("a2", "Device back online", null, "RESOLVED", ago(5400), AlertRow.AlertDevice("Warehouse Gateway")),
        ),
        devices = devices().map { DeviceLite(it.id, it.name, it.deviceId, it.status, it.lastSeen) },
    )

    fun createdDevice(name: String, type: String, proto: DeviceProtocol, location: String?): CreateDeviceResponse {
        val deviceId = name.lowercase().replace(" ", "-")
        val d = device(
            UUID.randomUUID().toString(), name, type.ifEmpty { "Generic" }, deviceId,
            proto.name, "OFFLINE", location, 0, null
        )
        val token = "demo_" + UUID.randomUUID().toString().replace("-", "").lowercase()
        return CreateDeviceResponse(d, token)
    }

    private fun device(
        id: String, name: String, type: String, deviceId: String, proto: String,
        status: String, location: String?, count: Int, lastSeen: String?,
    ): Device = Device(
        id = id, name = name, type = type, deviceId = deviceId, location = location,
        proto = proto, status = status, lastSeen = lastSeen,
        createdAt = Instant.now().minusSeconds(86400L * 12).toString(),
        count = DeviceCount(count),
    )
}
