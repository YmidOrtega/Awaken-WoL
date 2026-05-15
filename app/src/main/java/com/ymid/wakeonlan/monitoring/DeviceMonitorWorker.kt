package com.ymid.wakeonlan.monitoring

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ymid.wakeonlan.persistence.repository.DeviceRepository
import com.ymid.wakeonlan.ui.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class DeviceMonitorWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val devices = DeviceRepository.getInstance(appContext).all

        for (device in devices) {
            val ip = device.statusIp?.takeIf { it.isNotBlank() } ?: device.sshAddress ?: continue
            val nowOnline = pingHost(ip)
            val previouslyOnline = prefs.getBoolean(device.name, false)

            if (nowOnline && !previouslyOnline) {
                NotificationHelper.sendDeviceOnlineNotification(appContext, device.name)
            } else if (!nowOnline && previouslyOnline) {
                NotificationHelper.sendDeviceOfflineNotification(appContext, device.name)
            }

            prefs.edit().putBoolean(device.name, nowOnline).apply()
        }

        Result.success()
    }

    private fun pingHost(host: String): Boolean = try {
        InetAddress.getByName(host).isReachable(PING_TIMEOUT_MS)
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val PREFS_NAME = "device_monitor_state"
        private const val PING_TIMEOUT_MS = 2000
    }
}
