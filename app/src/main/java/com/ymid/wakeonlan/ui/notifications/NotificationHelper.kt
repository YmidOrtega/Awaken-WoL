package com.ymid.wakeonlan.ui.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ymid.wakeonlan.R

object NotificationHelper {

    const val CHANNEL_DEVICE_STATUS = "device_status"

    fun createChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_DEVICE_STATUS,
            context.getString(R.string.notification_channel_device_status),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_device_status_desc)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun sendDeviceOnlineNotification(context: Context, deviceName: String) {
        if (!hasPostNotificationsPermission(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_DEVICE_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_device_online_title, deviceName))
            .setContentText(context.getString(R.string.notification_device_online_body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(deviceName.hashCode(), n)
    }

    fun sendDeviceOfflineNotification(context: Context, deviceName: String) {
        if (!hasPostNotificationsPermission(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_DEVICE_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_device_offline_title, deviceName))
            .setContentText(context.getString(R.string.notification_device_offline_body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(deviceName.hashCode(), n)
    }

    private fun hasPostNotificationsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
