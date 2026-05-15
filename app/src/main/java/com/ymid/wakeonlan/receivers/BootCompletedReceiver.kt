package com.ymid.wakeonlan.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ymid.wakeonlan.monitoring.MonitoringScheduler

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        MonitoringScheduler.scheduleIfEnabled(context)
    }
}
