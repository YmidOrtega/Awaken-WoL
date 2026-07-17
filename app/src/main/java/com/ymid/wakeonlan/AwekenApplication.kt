package com.ymid.wakeonlan

import android.app.Application
import com.ymid.wakeonlan.config.RemoteConfigManager
import com.ymid.wakeonlan.crash.CrashReporter
import com.ymid.wakeonlan.monitoring.MonitoringScheduler
import com.ymid.wakeonlan.persistence.DatabaseInstanceManager
import com.ymid.wakeonlan.ui.notifications.NotificationHelper
import java.util.concurrent.Executors

class AwekenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.initialize()
        RemoteConfigManager.initialize()
        NotificationHelper.createChannels(this)
        MonitoringScheduler.scheduleIfEnabled(this)
        Executors.newSingleThreadExecutor().execute {
            runCatching { DatabaseInstanceManager.getInstance(this) }
        }
    }
}
