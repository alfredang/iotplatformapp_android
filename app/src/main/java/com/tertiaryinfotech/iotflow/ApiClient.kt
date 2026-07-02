package com.tertiaryinfotech.iotflow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Networking layer for the IoTFlow platform.
 *
 * The backend uses Auth.js (NextAuth v5) with a JWT session cookie. We log in by
 * replicating the browser credentials flow:
 *   1. GET  /api/auth/csrf                 -> csrfToken (+ csrf cookie)
 *   2. POST /api/auth/callback/credentials -> sets the session cookie
 * The [PersistentCookieJar] then carries the session cookie on every subsequent
 * JSON request, exactly like a browser would.
 */
object ApiClient {

    private lateinit var cookieJar: PersistentCookieJar
    private lateinit var client: OkHttpClient

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val JSON_MEDIA = "application/json".toMediaType()

    fun init() {
        cookieJar = PersistentCookieJar(Store.prefs())
        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(true)
            .build()
    }

    private fun url(path: String): String {
        val base = Store.serverURL.trimEnd('/')
        return base + path
    }

    // MARK: - Auth

    suspend fun login(email: String, password: String) = withContext(Dispatchers.IO) {
        val token = fetchCsrf()
        val form = FormBody.Builder()
            .add("csrfToken", token)
            .add("email", email)
            .add("password", password)
            .add("callbackUrl", "/dashboard")
            .add("json", "true")
            .build()
        val req = Request.Builder()
            .url(url("/api/auth/callback/credentials"))
            .header("X-Auth-Return-Redirect", "1")
            .post(form)
            .build()
        val (body, _) = send(req)
        val cb = runCatching { json.decodeFromString<CallbackResponse>(body) }.getOrNull()
        if (cb?.url?.contains("error") == true) throw ApiException("Invalid email or password.")
        if (currentUser() == null) throw ApiException("Invalid email or password.")
    }

    suspend fun register(name: String, email: String, password: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("name", name); put("email", email); put("password", password)
        }.toString()
        val req = Request.Builder()
            .url(url("/api/auth/register"))
            .post(payload.toRequestBody(JSON_MEDIA))
            .build()
        val (body, code) = send(req)
        if (code !in 200..299) {
            val msg = runCatching { json.decodeFromString<APIErrorResponse>(body).error }.getOrNull()
            throw ApiException(msg ?: "Registration failed ($code).")
        }
    }

    suspend fun currentUser(): SessionUser? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url("/api/auth/session")).build()
        val (body, _) = send(req)
        runCatching { json.decodeFromString<SessionResponse>(body).user }.getOrNull()
    }

    fun logout() {
        cookieJar.clear()
    }

    /** Permanently delete (anonymize) the signed-in account — Play/App Store policy. */
    suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        if (Store.demoMode) return@withContext
        val req = Request.Builder().url(url("/api/account")).delete().build()
        val (body, code) = send(req)
        if (code !in 200..299) {
            val msg = runCatching { json.decodeFromString<APIErrorResponse>(body).error }.getOrNull()
            throw ApiException(msg ?: "Could not delete account ($code).")
        }
        cookieJar.clear()
    }

    private fun fetchCsrf(): String {
        val req = Request.Builder().url(url("/api/auth/csrf")).build()
        val (body, _) = send(req)
        return runCatching { json.decodeFromString<CSRFResponse>(body).csrfToken }.getOrNull()
            ?: throw ApiException("Couldn't read the server response.")
    }

    // MARK: - Data

    suspend fun dashboardSummary(): DashboardSummary = withContext(Dispatchers.IO) {
        if (Store.demoMode) return@withContext DemoData.summary()
        getJson(url("/api/dashboard/summary"))
    }

    suspend fun devices(): List<Device> = withContext(Dispatchers.IO) {
        if (Store.demoMode) return@withContext DemoData.devices()
        val resp: DevicesResponse = getJson(url("/api/devices"))
        resp.devices
    }

    suspend fun createDevice(
        name: String, type: String, location: String?, proto: DeviceProtocol
    ): CreateDeviceResponse = withContext(Dispatchers.IO) {
        if (Store.demoMode) return@withContext DemoData.createdDevice(name, type, proto, location)
        val payload = JSONObject().apply {
            put("name", name)
            put("protocol", proto.name)
            if (type.isNotEmpty()) put("type", type)
            if (!location.isNullOrEmpty()) put("location", location)
        }.toString()
        val req = Request.Builder()
            .url(url("/api/devices"))
            .post(payload.toRequestBody(JSON_MEDIA))
            .build()
        val (body, code) = send(req)
        if (code !in 200..299) {
            val msg = runCatching { json.decodeFromString<APIErrorResponse>(body).error }.getOrNull()
            throw ApiException(msg ?: "Could not add device ($code).")
        }
        json.decodeFromString(body)
    }

    suspend fun deleteDevice(id: String) = withContext(Dispatchers.IO) {
        if (Store.demoMode) return@withContext
        val req = Request.Builder().url(url("/api/devices/$id")).delete().build()
        val (_, code) = send(req)
        if (code !in 200..299) throw ApiException("Could not delete device ($code).")
    }

    // MARK: - Dashboard widgets + device control

    /** The current project's dashboard widgets (display + control), matching web. */
    suspend fun dashboardWidgets(): List<DashWidget> = withContext(Dispatchers.IO) {
        if (Store.demoMode) return@withContext DemoData.widgets()
        val resp: WidgetsResponse = getJson(url("/api/dashboard/widgets"))
        resp.widgets
    }

    /** Latest value for a device metric (number / gauge / LED widgets). */
    suspend fun latestValue(deviceId: String, metric: String): Double? = withContext(Dispatchers.IO) {
        if (Store.demoMode) return@withContext DemoData.latest(deviceId, metric)
        val m = java.net.URLEncoder.encode(metric, "UTF-8")
        val resp: TelemetryLatestResponse = getJson(url("/api/devices/$deviceId/telemetry?metric=$m&limit=1"))
        resp.telemetry.firstOrNull()?.value
    }

    /** Current virtual-pin states for a device (control widgets reflect these). */
    suspend fun pinStates(deviceId: String): Map<String, Double> = withContext(Dispatchers.IO) {
        if (Store.demoMode) return@withContext DemoData.pins(deviceId)
        val resp: PinStateResponse = getJson(url("/api/devices/$deviceId/command"))
        resp.state.keys.mapNotNull { k -> resp.number(k)?.let { k to it } }.toMap()
    }

    /** Set a virtual-pin value (downlink control). */
    suspend fun setCommand(
        deviceId: String, pin: String, value: Double?, strValue: String? = null,
    ) = withContext(Dispatchers.IO) {
        if (Store.demoMode) { DemoData.setPin(deviceId, pin, value); return@withContext }
        val payload = JSONObject().apply {
            put("pin", pin)
            if (value != null) put("value", value)
            if (strValue != null) put("strValue", strValue)
        }.toString()
        val req = Request.Builder()
            .url(url("/api/devices/$deviceId/command"))
            .post(payload.toRequestBody(JSON_MEDIA))
            .build()
        val (body, code) = send(req)
        if (code !in 200..299) {
            val msg = runCatching { json.decodeFromString<APIErrorResponse>(body).error }.getOrNull()
            throw ApiException(msg ?: "Could not send command ($code).")
        }
    }

    /** Recent telemetry history for a metric (chart widgets), oldest first. */
    suspend fun telemetryHistory(deviceId: String, metric: String, limit: Int = 30): List<TelemetryHistoryResponse.TelemetryPoint> =
        withContext(Dispatchers.IO) {
            if (Store.demoMode) return@withContext DemoData.history(metric)
            val m = java.net.URLEncoder.encode(metric, "UTF-8")
            val resp: TelemetryHistoryResponse = getJson(url("/api/devices/$deviceId/telemetry?metric=$m&limit=$limit"))
            resp.telemetry.reversed() // API returns newest first
        }

    // MARK: - Automations (n8n)

    /** The current project's n8n automations. */
    suspend fun automations(): List<Automation> = withContext(Dispatchers.IO) {
        if (Store.demoMode) return@withContext DemoData.automations()
        val resp: AutomationsResponse = getJson(url("/api/automations"))
        resp.automations
    }

    /** Send a sample event to the automation's n8n webhook — same as the web Test button. */
    suspend fun triggerAutomation(id: String): AutomationActionResponse = withContext(Dispatchers.IO) {
        if (Store.demoMode) return@withContext DemoData.triggerAutomation(id)
        val req = Request.Builder()
            .url(url("/api/automations/$id"))
            .post("".toRequestBody(JSON_MEDIA))
            .build()
        val (body, code) = send(req)
        if (code !in 200..299) {
            val msg = runCatching { json.decodeFromString<APIErrorResponse>(body).error }.getOrNull()
            throw ApiException(msg ?: "Could not trigger flow ($code).")
        }
        runCatching { json.decodeFromString<AutomationActionResponse>(body) }
            .getOrDefault(AutomationActionResponse(ok = true))
    }

    /** Enable or disable an automation. */
    suspend fun setAutomationEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        if (Store.demoMode) { DemoData.setAutomationEnabled(id, enabled); return@withContext }
        val payload = JSONObject().apply { put("enabled", enabled) }.toString()
        val req = Request.Builder()
            .url(url("/api/automations/$id"))
            .patch(payload.toRequestBody(JSON_MEDIA))
            .build()
        val (body, code) = send(req)
        if (code !in 200..299) {
            val msg = runCatching { json.decodeFromString<APIErrorResponse>(body).error }.getOrNull()
            throw ApiException(msg ?: "Could not update automation ($code).")
        }
    }

    // MARK: - Helpers

    private inline fun <reified T> getJson(fullUrl: String): T {
        val req = Request.Builder().url(fullUrl).build()
        val (body, code) = send(req)
        if (code !in 200..299) {
            val msg = runCatching { json.decodeFromString<APIErrorResponse>(body).error }.getOrNull()
            throw ApiException(msg ?: "Request failed ($code).")
        }
        return try {
            json.decodeFromString(body)
        } catch (_: Exception) {
            throw ApiException("Couldn't read the server response.")
        }
    }

    private fun send(req: Request): Pair<String, Int> {
        try {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                return body to resp.code
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException(e.localizedMessage ?: "Network error.")
        }
    }
}
