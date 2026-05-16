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

/**
 * Manages FLEXIBLE in-app update prompts via the Google Play Core API.
 *
 * Lifecycle:
 *   Activity.onCreate  → attach(activity) + checkForUpdate(activity)
 *   Activity.onResume  → resumeIfUpdateDownloaded(activity)
 *   Activity.onDestroy → detach()
 *
 * FLEXIBLE means: the update downloads silently in the background while the user
 * keeps using the app; a Snackbar prompts to restart once the download is done.
 * The whole flow is skipped when the remote flag `in_app_updates_enabled` is false.
 */
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
                    // Play Store not available or update flow already active — skip silently.
                }
            }
        }
    }

    /**
     * Call from onResume to catch updates that finished downloading while the app
     * was in the background or the user dismissed the initial prompt.
     */
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
