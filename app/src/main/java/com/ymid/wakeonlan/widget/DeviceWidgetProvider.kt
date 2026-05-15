package com.ymid.wakeonlan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ymid.wakeonlan.R
import com.ymid.wakeonlan.persistence.repository.DeviceRepository
import com.ymid.wakeonlan.receivers.WakeDeviceReceiver
import java.util.concurrent.Executors

class DeviceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Executors.newSingleThreadExecutor().execute {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            for (id in ids) {
                val deviceId = prefs.getInt(widgetKey(id), -1)
                updateWidget(context, manager, id, deviceId)
            }
        }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ids.forEach { prefs.edit().remove(widgetKey(it)).apply() }
    }

    companion object {
        const val PREFS = "widget_prefs"

        fun widgetKey(widgetId: Int) = "widget_device_$widgetId"

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int, deviceId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_device)

            if (deviceId == -1) {
                views.setTextViewText(R.id.widget_device_name, context.getString(R.string.widget_device_none))
                views.setTextViewText(R.id.widget_wake_button, context.getString(R.string.widget_wake))
            } else {
                val device = runCatching { DeviceRepository.getInstance(context).getById(deviceId) }.getOrNull()
                if (device != null) {
                    views.setTextViewText(R.id.widget_device_name, device.name)

                    val wakeIntent = Intent(context, WakeDeviceReceiver::class.java).apply {
                        action = "com.ymid.wakeonlan.ACTION_WAKE"
                        putExtra(WakeDeviceReceiver.EXTRA_DEVICE_ID, deviceId)
                    }
                    val wakePi = PendingIntent.getBroadcast(
                        context, widgetId, wakeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_wake_button, wakePi)
                    views.setTextViewText(R.id.widget_wake_button, context.getString(R.string.widget_wake))
                } else {
                    views.setTextViewText(R.id.widget_device_name, context.getString(R.string.widget_device_none))
                }
            }

            manager.updateAppWidget(widgetId, views)
        }
    }
}
