package com.tertiaryinfotech.iotflow

import android.content.Context
import android.content.SharedPreferences

object AppConfig {
    const val DEFAULT_SERVER = "https://iot.tertiaryinfotech.com"
}

/** Lightweight key-value store mirroring the iOS `UserDefaults` usage. */
object Store {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("iotflow", Context.MODE_PRIVATE)
    }

    fun prefs(): SharedPreferences = prefs

    var serverURL: String
        get() = prefs.getString("serverURL", AppConfig.DEFAULT_SERVER) ?: AppConfig.DEFAULT_SERVER
        set(value) = prefs.edit().putString("serverURL", value).apply()

    var demoMode: Boolean
        get() = prefs.getBoolean("demoMode", false)
        set(value) = prefs.edit().putBoolean("demoMode", value).apply()
}
