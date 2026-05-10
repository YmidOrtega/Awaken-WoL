package com.ymid.wakeonlan.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public final class LauncherIconManager {
    private static final String TAG = "LauncherIconManager";
    // Temporary flag to disable runtime launcher alias toggling while debugging device-specific issues
    private static final boolean DISABLE_LAUNCHER_TOGGLE = true;

    private static final String ALIAS_ON = "com.ymid.wakeonlan.ui.MainActivityLauncherOn";
    private static final String ALIAS_OFF = "com.ymid.wakeonlan.ui.MainActivityLauncherOff";

    private LauncherIconManager() {
    }

    public static void updateLauncherIcon(Context context, boolean deviceOnline) {
        if (DISABLE_LAUNCHER_TOGGLE) {
            Log.i(TAG, "Launcher icon toggle disabled (temporary). Requested state: " + deviceOnline);
            return;
        }

        PackageManager packageManager = context.getPackageManager();
        ComponentName onAlias = new ComponentName(context, ALIAS_ON);
        ComponentName offAlias = new ComponentName(context, ALIAS_OFF);

        int enableState = deviceOnline
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        int disableState = deviceOnline
                ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

        try {
            // Apply requested state to both aliases
            packageManager.setComponentEnabledSetting(onAlias, enableState, PackageManager.DONT_KILL_APP);
            packageManager.setComponentEnabledSetting(offAlias, disableState, PackageManager.DONT_KILL_APP);

            // Safety check: ensure at least one launcher alias remains enabled to avoid removing the app icon entirely
            int onState = packageManager.getComponentEnabledSetting(onAlias);
            int offState = packageManager.getComponentEnabledSetting(offAlias);
            Log.i(TAG, "Launcher alias states after set: on=" + onState + " off=" + offState);

            if (onState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    && offState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                // Neither explicitly enabled — enable the OFF alias as a fallback
                Log.w(TAG, "No launcher alias enabled, re-enabling OFF alias as fallback");
                packageManager.setComponentEnabledSetting(offAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException while toggling launcher aliases", se);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while toggling launcher aliases", e);
        }
    }
}
