package com.ymid.wakeonlan.ui;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    /**
     * Tracks whether any activity of the app is visible. Switching the launcher alias removes the
     * task, so icon changes wait until the whole app is in the background, not just MainActivity
     * (opening Settings or the device editor used to close the app).
     */
    public static void register(Application application) {
        appContext = application.getApplicationContext();
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private int startedActivities = 0;

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                startedActivities++;
                setAppInForeground(true);
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                startedActivities = Math.max(0, startedActivities - 1);
                if (startedActivities == 0 && !activity.isChangingConfigurations()) {
                    setAppInForeground(false);
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    private static void setAppInForeground(boolean inForeground) {
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
