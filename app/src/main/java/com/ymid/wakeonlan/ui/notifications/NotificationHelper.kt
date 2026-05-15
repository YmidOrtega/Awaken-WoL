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
    const val CHANNEL_ACTIONS = "actions"

    fun createChannels(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DEVICE_STATUS,
                context.getString(R.string.notification_channel_device_status),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.notification_channel_device_status_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ACTIONS,
                context.getString(R.string.notification_channel_actions),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = context.getString(R.string.notification_channel_actions_desc) }
        )
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

    fun sendWakeSentNotification(context: Context, deviceName: String) {
        if (!hasPostNotificationsPermission(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_ACTIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_wake_sent_title))
            .setContentText(context.getString(R.string.notification_wake_sent_body, deviceName))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(("wake_$deviceName").hashCode(), n)
    }

    fun sendShutdownSentNotification(context: Context, deviceName: String) {
        if (!hasPostNotificationsPermission(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_ACTIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_shutdown_sent_title))
            .setContentText(context.getString(R.string.notification_shutdown_sent_body, deviceName))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(("shutdown_$deviceName").hashCode(), n)
    }

    private fun hasPostNotificationsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
