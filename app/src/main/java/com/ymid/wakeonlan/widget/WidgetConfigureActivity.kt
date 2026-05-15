package com.ymid.wakeonlan.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ymid.wakeonlan.R
import com.ymid.wakeonlan.persistence.repository.DeviceRepository
import java.util.concurrent.Executors

class WidgetConfigureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        Executors.newSingleThreadExecutor().execute {
            val devices = DeviceRepository.getInstance(this).getAll()
            runOnUiThread {
                if (devices.isEmpty()) {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.widget_configure_no_devices)
                        .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                        .setOnCancelListener { finish() }
                        .show()
                    return@runOnUiThread
                }

                val names = devices.map { it.name }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle(R.string.widget_configure_title)
                    .setItems(names) { _, index ->
                        val device = devices[index]
                        getSharedPreferences(DeviceWidgetProvider.PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putInt(DeviceWidgetProvider.widgetKey(widgetId), device.id)
                            .apply()
                        val manager = AppWidgetManager.getInstance(this)
                        DeviceWidgetProvider.updateWidget(this, manager, widgetId, device.id)
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                        finish()
                    }
                    .setOnCancelListener { finish() }
                    .show()
            }
        }
    }
}
