package com.ymid.wakeonlan.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.ymid.wakeonlan.monitoring.MonitoringScheduler

class PackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        resetAliasesToDefault(context)
        MonitoringScheduler.scheduleIfEnabled(context)
    }

    companion object {
        private const val ALIAS_OFF = "com.ymid.wakeonlan.ui.MainActivityLauncherOff"
        private const val ALIAS_ON = "com.ymid.wakeonlan.ui.MainActivityLauncherOn"

        fun resetAliasesToDefault(context: Context) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                ComponentName(context, ALIAS_OFF),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                ComponentName(context, ALIAS_ON),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
