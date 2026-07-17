package com.ymid.wakeonlan.update

import android.app.Activity
import android.content.IntentSender
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.ymid.wakeonlan.R
import com.ymid.wakeonlan.config.RemoteConfigManager

object InAppUpdateManager {

    private const val UPDATE_REQUEST_CODE = 900

    private var appUpdateManager: AppUpdateManager? = null
    private var installStateListener: InstallStateUpdatedListener? = null

    // ── Lifecycle API (called from MainActivity) ──────────────────────────────

    @JvmStatic
    fun attach(activity: Activity) {
        if (!RemoteConfigManager.isInAppUpdatesEnabled()) return

        val manager = AppUpdateManagerFactory.create(activity)
        appUpdateManager = manager

        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartSnackbar(activity, manager)
            }
        }
        installStateListener = listener
        manager.registerListener(listener)
    }

    @JvmStatic
    fun checkForUpdate(activity: Activity) {
        val manager = appUpdateManager ?: return

        manager.appUpdateInfo.addOnSuccessListener { info ->
            val canUpdate = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

            if (canUpdate) {
                try {
                    manager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        UPDATE_REQUEST_CODE
                    )
                } catch (_: IntentSender.SendIntentException) {
                }
            }
        }
    }

    @JvmStatic
    fun resumeIfUpdateDownloaded(activity: Activity) {
        val manager = appUpdateManager ?: return

        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartSnackbar(activity, manager)
            }
        }
    }

    @JvmStatic
    fun detach() {
        installStateListener?.let { appUpdateManager?.unregisterListener(it) }
        appUpdateManager = null
        installStateListener = null
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun showRestartSnackbar(activity: Activity, manager: AppUpdateManager) {
        val rootView: View = activity.findViewById(android.R.id.content) ?: return
        Snackbar.make(rootView, R.string.update_downloaded_message, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.update_downloaded_action) { manager.completeUpdate() }
            .show()
    }
}
