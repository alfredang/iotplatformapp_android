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
