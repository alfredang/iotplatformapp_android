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

    // MARK: - Dashboard widgets + control (demo)

    /** In-memory virtual-pin state so demo control widgets feel live. */
    private val pinStore = mutableMapOf(
        "d1|V1" to 1.0,          // Living Room LED on
        "d1|brightness" to 60.0,
        "d2|relay" to 0.0,       // Warehouse fan off
    )

    fun widgets(): List<DashWidget> = listOf(
        widget("w1", "NUMBER", "Living Room Temp", "d1", "Living Room Sensor", metric = "temperature"),
        widget("w2", "GAUGE", "Humidity", "d1", "Living Room Sensor", metric = "humidity", min = 0.0, max = 100.0),
        widget("w3", "SWITCH", "Living Room Light", "d1", "Living Room Sensor", pin = "V1"),
        widget("w4", "SLIDER", "Brightness", "d1", "Living Room Sensor", pin = "brightness", min = 0.0, max = 100.0),
        widget("w5", "SWITCH", "Warehouse Fan", "d2", "Warehouse Gateway", pin = "relay"),
        widget("w6", "BUTTON", "Ring Buzzer", "d2", "Warehouse Gateway", pin = "buzzer"),
        widget("w7", "STATUS", "Rooftop Weather", "d3", "Rooftop Weather"),
        widget("w8", "LED", "Pump Running", "d2", "Warehouse Gateway", metric = "power_kw"),
    )

    fun latest(deviceId: String, metric: String): Double = when (metric) {
        "temperature" -> 22.4
        "humidity" -> 48.0
        "power_kw" -> 3.7
        "wind_ms" -> 5.1
        else -> 12.3
    }

    fun pins(deviceId: String): Map<String, Double> =
        pinStore.filterKeys { it.startsWith("$deviceId|") }
            .mapKeys { it.key.substringAfter("|") }

    fun setPin(deviceId: String, pin: String, value: Double?) {
        pinStore["$deviceId|$pin"] = value ?: 0.0
    }

    /** Deterministic wave so demo chart widgets look alive. */
    fun history(metric: String): List<TelemetryHistoryResponse.TelemetryPoint> {
        val base = latest("", metric)
        return (0 until 30).map { i ->
            val wobble = kotlin.math.sin(i / 4.0) * base * 0.08 + (i % 5) * base * 0.01
            TelemetryHistoryResponse.TelemetryPoint(ago((30 - i) * 300L), base + wobble)
        }
    }

    // MARK: - Automations / n8n (demo)

    private val automationEnabled = mutableMapOf<String, Boolean>()

    fun automations(): List<Automation> = listOf(
        automation("au1", "Overheat → n8n email", "ALERT", "temperature", ago(1800), "200"),
        automation("au2", "Log telemetry to sheet", "TELEMETRY", "humidity", ago(300), "200"),
        automation("au3", "Fan command webhook", "COMMAND", null, ago(4200), "200"),
        automation("au4", "Notify device offline", "DEVICE_OFFLINE", null, ago(7200), "500"),
        automation("au5", "Welcome device online", "DEVICE_ONLINE", null, null, null),
    )

    fun triggerAutomation(id: String): AutomationActionResponse = AutomationActionResponse(ok = true, status = "200")

    fun setAutomationEnabled(id: String, enabled: Boolean) {
        automationEnabled[id] = enabled
    }

    private fun automation(
        id: String, name: String, event: String, metric: String?,
        lastFiredAt: String?, lastStatus: String?,
    ): Automation = Automation(
        id = id, name = name, event = event, metric = metric,
        n8nWebhookUrl = "https://n8n.tertiarytraining.com/webhook/iot-demo-$id",
        enabled = automationEnabled[id] ?: (id != "au5"),
        lastFiredAt = lastFiredAt, lastStatus = lastStatus,
    )

    private fun widget(
        id: String, type: String, title: String, devId: String, devName: String,
        metric: String? = null, pin: String? = null, min: Double? = null, max: Double? = null,
    ): DashWidget = DashWidget(
        id = id, type = type, title = title, metric = metric,
        config = if (pin != null || min != null || max != null) WidgetConfig(min, max, pin) else null,
        device = WidgetDevice(devId, devName, devName.lowercase().replace(" ", "-"), "ONLINE"),
    )

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
