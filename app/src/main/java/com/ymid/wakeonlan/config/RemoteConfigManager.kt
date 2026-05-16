package com.ymid.wakeonlan.config

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.ymid.wakeonlan.BuildConfig
import com.ymid.wakeonlan.R

/**
 * Central access point for Firebase Remote Config feature flags.
 *
 * All flags have safe local defaults so the app works correctly when Firebase
 * is not configured (CI builds, forks) or when the device is offline.
 *
 * Flag values propagate after the next cold start once fetchAndActivate()
 * completes — they are never applied mid-session to avoid runtime surprises.
 */
object RemoteConfigManager {

    private val firebaseAvailable = BuildConfig.FIREBASE_CONFIGURED

    // ── Flag keys (match remote_config_defaults.xml and the Firebase console) ─

    const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    const val KEY_PING_STATUS_ENABLED = "ping_status_enabled"
    const val KEY_NETWORK_SCAN_ENABLED = "network_scan_enabled"
    const val KEY_SSH_SHUTDOWN_ENABLED = "ssh_shutdown_enabled"
    const val KEY_IN_APP_UPDATES_ENABLED = "in_app_updates_enabled"
    const val KEY_PING_TIMEOUT_MS = "ping_timeout_ms"

    fun initialize() {
        if (!firebaseAvailable) return

        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
                    .build()
            )
            setDefaultsAsync(R.xml.remote_config_defaults)
            fetchAndActivate()
        }
    }

    // ── Flag getters ──────────────────────────────────────────────────────────

    @JvmStatic
    fun isMonitoringEnabled(): Boolean = flag(KEY_MONITORING_ENABLED, default = true)

    @JvmStatic
    fun isPingStatusEnabled(): Boolean = flag(KEY_PING_STATUS_ENABLED, default = true)

    @JvmStatic
    fun isNetworkScanEnabled(): Boolean = flag(KEY_NETWORK_SCAN_ENABLED, default = true)

    @JvmStatic
    fun isSshShutdownEnabled(): Boolean = flag(KEY_SSH_SHUTDOWN_ENABLED, default = true)

    @JvmStatic
    fun isInAppUpdatesEnabled(): Boolean = flag(KEY_IN_APP_UPDATES_ENABLED, default = true)

    @JvmStatic
    fun getPingTimeoutMs(): Long = longValue(KEY_PING_TIMEOUT_MS, default = 2000L)

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun flag(key: String, default: Boolean): Boolean =
        if (firebaseAvailable) Firebase.remoteConfig.getBoolean(key) else default

    private fun longValue(key: String, default: Long): Long =
        if (firebaseAvailable) Firebase.remoteConfig.getLong(key) else default
}
