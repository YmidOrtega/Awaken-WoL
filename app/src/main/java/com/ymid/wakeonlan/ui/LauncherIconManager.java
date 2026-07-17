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
    private static volatile Boolean pendingState = null;
    private static volatile boolean appInForeground = false;
    private static volatile Context appContext = null;
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    private LauncherIconManager() {
    }

    public static void setAppInForeground(Context context, boolean inForeground) {
        appContext = context.getApplicationContext();
        appInForeground = inForeground;

        if (!inForeground && pendingState != null) {
            Boolean state = pendingState;
            pendingState = null;
            EXECUTOR.execute(() -> applyIconState(appContext, state));
        }
    }

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
