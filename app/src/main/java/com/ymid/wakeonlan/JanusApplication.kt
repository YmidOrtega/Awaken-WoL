package com.ymid.wakeonlan

import android.app.Application
import com.ymid.wakeonlan.config.RemoteConfigManager
import com.ymid.wakeonlan.crash.CrashReporter
import com.ymid.wakeonlan.monitoring.LauncherIconWorker
import com.ymid.wakeonlan.monitoring.MonitoringScheduler
import com.ymid.wakeonlan.persistence.DatabaseInstanceManager
import com.ymid.wakeonlan.ui.LauncherIconManager
import com.ymid.wakeonlan.ui.notifications.NotificationHelper
import java.util.concurrent.Executors

class JanusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LauncherIconManager.register(this)
        CrashReporter.initialize()
        RemoteConfigManager.initialize()
        NotificationHelper.createChannels(this)
        MonitoringScheduler.scheduleIfEnabled(this)
        LauncherIconWorker.schedule(this)
        Executors.newSingleThreadExecutor().execute {
            runCatching { DatabaseInstanceManager.getInstance(this) }
        }
    }
}
