package com.ymid.wakeonlan.monitoring

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ymid.wakeonlan.persistence.repository.DeviceRepository
import com.ymid.wakeonlan.ui.LauncherIconManager
import com.ymid.wakeonlan.ui.list.DeviceListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Keeps the launcher icon in sync with the first listed device while the app is closed. The list
 * only pings while it is on screen, so without this the icon went stale until the app was opened.
 * Independent of the monitoring notifications, which the user can turn off.
 */
class LauncherIconWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val devices = DeviceRepository.getInstance(appContext).all
        val device = DeviceListAdapter.firstDisplayedDevice(devices) ?: return@withContext Result.success()
        // Without an address there is no status to show; leave the icon as it is
        val host = HostReachability.statusHost(device) ?: return@withContext Result.success()

        LauncherIconManager.updateLauncherIcon(appContext, HostReachability.isReachable(host))
        Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "launcher_icon_sync"
        private const val FOLLOW_UP_WORK_NAME = "launcher_icon_follow_up"

        // A PC takes a while to boot or power off; check once early and once when it has surely settled
        private val FOLLOW_UP_DELAYS_SECONDS = longArrayOf(45, 120)

        private val CONSTRAINTS = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        @JvmStatic
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LauncherIconWorker>(15, TimeUnit.MINUTES)
                .setConstraints(CONSTRAINTS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Re-checks soon after a wake or shutdown so the icon follows without waiting for the periodic run. */
        @JvmStatic
        fun scheduleFollowUp(context: Context) {
            val requests = FOLLOW_UP_DELAYS_SECONDS.map { delay ->
                OneTimeWorkRequestBuilder<LauncherIconWorker>()
                    .setInitialDelay(delay, TimeUnit.SECONDS)
                    .setConstraints(CONSTRAINTS)
                    .build()
            }
            WorkManager.getInstance(context).enqueueUniqueWork(
                FOLLOW_UP_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                requests
            )
        }
    }
}
