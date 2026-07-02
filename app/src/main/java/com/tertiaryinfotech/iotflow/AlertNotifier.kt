package com.tertiaryinfotech.iotflow

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Polls the dashboard summary every 30s while the app is in the foreground and
 * posts a local notification when a new ACTIVE alert appears — the Android
 * equivalent of the iOS NotificationManager (no push infrastructure; the first
 * poll primes the seen-set so existing alerts don't fire).
 */
object AlertNotifier {
    const val POLL_INTERVAL_MS = 30_000L
    private const val CHANNEL_ID = "alerts"
    private val seen = mutableSetOf<String>()
    private var primed = false

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL_ID, "Device alerts", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Alerts raised by your IoT devices" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    suspend fun poll(context: Context) {
        val summary = runCatching { ApiClient.dashboardSummary() }.getOrNull() ?: return
        val active = summary.recentAlerts.filter { it.status == "ACTIVE" }
        if (!primed) {
            seen.addAll(active.map { it.id })
            primed = true
            return
        }
        for (alert in active) {
            if (seen.add(alert.id)) notify(context, alert)
        }
    }

    @SuppressLint("MissingPermission")
    private fun notify(context: Context, alert: AlertRow) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle(alert.device?.name ?: "Device alert")
            .setContentText(alert.message)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(alert.id.hashCode(), notification)
    }
}
