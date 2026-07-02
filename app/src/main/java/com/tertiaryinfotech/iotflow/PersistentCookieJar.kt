package com.tertiaryinfotech.iotflow

import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Cookie storage backed by SharedPreferences so the Auth.js session cookie
 * survives app restarts — the Android equivalent of iOS `HTTPCookieStorage.shared`.
 */
class PersistentCookieJar(private val prefs: SharedPreferences) : CookieJar {
    private val cache = mutableListOf<Cookie>()
    private val sep = "\u0001" // control char: never appears in cookie fields

    init {
        prefs.getString("cookies", null)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { deserialize(it) }
            ?.let { cache.addAll(it) }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        for (c in cookies) {
            cache.removeAll { it.name == c.name && it.domain == c.domain && it.path == c.path }
            if (c.expiresAt > now) cache.add(c)
        }
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        cache.removeAll { it.expiresAt < now }
        return cache.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        cache.clear()
        prefs.edit().remove("cookies").apply()
    }

    private fun persist() {
        prefs.edit().putString("cookies", cache.joinToString("\n") { serialize(it) }).apply()
    }

    private fun serialize(c: Cookie): String = listOf(
        c.name, c.value, c.expiresAt.toString(), c.domain, c.path,
        c.secure.toString(), c.httpOnly.toString(), c.hostOnly.toString()
    ).joinToString(sep)

    private fun deserialize(s: String): Cookie? {
        val p = s.split(sep)
        if (p.size < 8) return null
        return try {
            val b = Cookie.Builder()
                .name(p[0]).value(p[1]).path(p[4]).expiresAt(p[2].toLong())
            if (p[7].toBoolean()) b.hostOnlyDomain(p[3]) else b.domain(p[3])
            if (p[5].toBoolean()) b.secure()
            if (p[6].toBoolean()) b.httpOnly()
            b.build()
        } catch (_: Exception) {
            null
        }
    }
}
