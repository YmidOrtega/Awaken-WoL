package com.ymid.wakeonlan.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ymid.wakeonlan.BuildConfig

object CrashReporter {

    private val enabled = BuildConfig.FIREBASE_CONFIGURED

    fun initialize() {
        if (!enabled) return

        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            setCustomKey("version_name", BuildConfig.VERSION_NAME)
            setCustomKey("version_code", BuildConfig.VERSION_CODE)
            log("Crashlytics initialized")
        }
    }

    fun log(message: String) {
        if (!enabled) return
        FirebaseCrashlytics.getInstance().log(message)
    }

    fun recordException(throwable: Throwable) {
        if (!enabled) return
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }
}
