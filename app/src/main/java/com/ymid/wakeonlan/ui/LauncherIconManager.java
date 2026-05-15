package com.ymid.wakeonlan.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class LauncherIconManager {
    private static final String TAG = "LauncherIconManager";

    private static final String ALIAS_ON = "com.ymid.wakeonlan.ui.MainActivityLauncherOn";
    private static final String ALIAS_OFF = "com.ymid.wakeonlan.ui.MainActivityLauncherOff";

    private static volatile Boolean lastAppliedState = null;
    // State waiting to be applied once the app goes to the background.
    private static volatile Boolean pendingState = null;
    // Whether any activity of this app is currently visible to the user.
    private static volatile boolean appInForeground = false;
    // Kept so we can apply a pending change when the app leaves the foreground.
    private static volatile Context appContext = null;

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    private LauncherIconManager() {
    }

    /**
     * Call from every Activity's onResume / onPause to track foreground state.
     * When the app goes to the background any pending icon change is applied,
     * so the launcher icon never changes while the user is actively using the app
     * (which would cause Samsung / MIUI launchers to restart the visible activity).
     */
    public static void setAppInForeground(Context context, boolean inForeground) {
        appContext = context.getApplicationContext();
        appInForeground = inForeground;

        if (!inForeground && pendingState != null) {
            Boolean state = pendingState;
            pendingState = null;
            EXECUTOR.execute(() -> applyIconState(appContext, state));
        }
    }

    /**
     * Call from MainActivity.onCreate() to recover from any corrupted alias state
     * (both aliases disabled = app icon disappears from the launcher).
     */
    public static void ensureValidState(Context context) {
        EXECUTOR.execute(() -> {
            PackageManager pm = context.getApplicationContext().getPackageManager();
            ComponentName offAlias = new ComponentName(context, ALIAS_OFF);
            ComponentName onAlias = new ComponentName(context, ALIAS_ON);
            try {
                int offState = pm.getComponentEnabledSetting(offAlias);
                int onState = pm.getComponentEnabledSetting(onAlias);

                boolean offDisabled = offState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                boolean onActive = onState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

                // Recover if Off is explicitly disabled but On is not active.
                // This happens when the launcher caches a shortcut to Off after an APK update
                // while Off was left disabled by a previous LauncherIconManager toggle.
                if (offDisabled && !onActive) {
                    Log.w(TAG, "Launcher stuck: Off disabled, On inactive — recovering");
                    pm.setComponentEnabledSetting(offAlias,
                            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                            PackageManager.DONT_KILL_APP);
                    pm.setComponentEnabledSetting(onAlias,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                    lastAppliedState = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in ensureValidState", e);
            }
        });
    }

    public static void updateLauncherIcon(Context context, boolean deviceOnline) {
        if (Boolean.valueOf(deviceOnline).equals(lastAppliedState)) {
            return;
        }
        lastAppliedState = deviceOnline;

        if (appInForeground) {
            // Defer: applying setComponentEnabledSetting while the app is visible causes
            // Samsung / MIUI launchers to restart the foreground activity.
            pendingState = deviceOnline;
            Log.i(TAG, "Icon update deferred (app in foreground) — state=" + deviceOnline);
            return;
        }

        EXECUTOR.execute(() -> applyIconState(context.getApplicationContext(), deviceOnline));
    }

    private static void applyIconState(Context context, boolean deviceOnline) {
        PackageManager pm = context.getPackageManager();
        ComponentName onAlias = new ComponentName(context, ALIAS_ON);
        ComponentName offAlias = new ComponentName(context, ALIAS_OFF);

        try {
            // Enable the incoming alias BEFORE disabling the outgoing one so there is
            // never a window where both are disabled (which removes the icon from the launcher).
            if (deviceOnline) {
                pm.setComponentEnabledSetting(onAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(offAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } else {
                pm.setComponentEnabledSetting(offAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(onAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
            Log.i(TAG, "Launcher icon applied — deviceOnline=" + deviceOnline);
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException while toggling launcher aliases", se);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while toggling launcher aliases", e);
        }
    }
}
