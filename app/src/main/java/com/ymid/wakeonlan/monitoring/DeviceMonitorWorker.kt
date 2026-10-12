package com.ymid.wakeonlan.monitoring

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ymid.wakeonlan.persistence.repository.DeviceRepository
import com.ymid.wakeonlan.ping.Ping
import com.ymid.wakeonlan.ui.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class DeviceMonitorWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!MonitoringScheduler.isEnabled(appContext)) {
            return@withContext Result.success()
        }

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val devices = DeviceRepository.getInstance(appContext).all

        for (device in devices) {
            val ip = device.statusIp?.takeIf { it.isNotBlank() } ?: device.sshAddress ?: continue
            val nowOnline = pingHost(ip)
            val stateKey = stateKey(device.id)
            val hasPreviousState = prefs.contains(stateKey)
            val previouslyOnline = prefs.getBoolean(stateKey, false)

            if (hasPreviousState && nowOnline && !previouslyOnline) {
                NotificationHelper.sendDeviceOnlineNotification(appContext, device.name)
            } else if (hasPreviousState && !nowOnline && previouslyOnline) {
                NotificationHelper.sendDeviceOfflineNotification(appContext, device.name)
            }

            prefs.edit().putBoolean(stateKey, nowOnline).apply()
        }

        Result.success()
    }

    private fun pingHost(host: String): Boolean {
        val address = try {
            InetAddress.getByName(host)
        } catch (_: Exception) {
            return false
        }
        return (1..PING_ATTEMPTS).any { icmpPing(address) ?: fallbackReachable(address) }
    }

    private fun icmpPing(address: InetAddress): Boolean? {
        var result: Boolean? = null
        Ping(address, object : Ping.PingListener {
            override fun onPing(timeMs: Long) {
                result = timeMs != Ping.TIMED_OUT_MS
            }

            override fun onPingException(e: Exception) {
                result = null
            }
        }).apply { timeoutMs = PING_TIMEOUT_MS }.run()
        return result
    }

    private fun fallbackReachable(address: InetAddress): Boolean = try {
        address.isReachable(PING_TIMEOUT_MS)
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val PREFS_NAME = "device_monitor_state"
        private const val PING_TIMEOUT_MS = 2000
        private const val PING_ATTEMPTS = 2

        private fun stateKey(deviceId: Int) = "device_$deviceId"
    }
}
