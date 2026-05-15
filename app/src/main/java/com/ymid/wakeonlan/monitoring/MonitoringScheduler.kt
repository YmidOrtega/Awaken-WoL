package com.ymid.wakeonlan.monitoring

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.preference.PreferenceManager
import java.util.concurrent.TimeUnit

object MonitoringScheduler {

    const val PREF_MONITORING_ENABLED = "pref_monitoring_enabled"
    private const val WORK_NAME = "device_monitor"

    @JvmStatic
    fun scheduleIfEnabled(context: Context) {
        if (isEnabled(context)) {
            schedule(context)
        } else {
            cancel(context)
        }
    }

    @JvmStatic
    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_MONITORING_ENABLED, true)

    @JvmStatic
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DeviceMonitorWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    @JvmStatic
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
