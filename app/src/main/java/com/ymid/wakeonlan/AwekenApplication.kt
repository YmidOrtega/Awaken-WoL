package com.ymid.wakeonlan

import android.app.Application
import com.ymid.wakeonlan.monitoring.MonitoringScheduler
import com.ymid.wakeonlan.ui.notifications.NotificationHelper

class AwekenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        MonitoringScheduler.schedule(this)
    }
}
