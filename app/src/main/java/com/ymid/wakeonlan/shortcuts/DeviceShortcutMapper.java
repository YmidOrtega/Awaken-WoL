package com.ymid.wakeonlan.shortcuts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Arrays;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.models.Device;

public class DeviceShortcutMapper {

    /** Same capability as res/xml/shortcuts.xml; binding lets assistants match the exact phrase. */
    private static final String OPEN_APP_FEATURE = "actions.intent.OPEN_APP_FEATURE";
    private static final String FEATURE_PARAMETER = "feature";

    public static ShortcutInfoCompat buildWakeShortcut(Device device, Context context) {
        return new ShortcutInfoCompat.Builder(context, "wake_" + device.id)
                .setShortLabel(context.getString(R.string.shortcut_label_wake, device.name))
                .setLongLabel(context.getString(R.string.shortcut_label_wake, device.name))
                .setIntent(buildWakeIntent(device, context))
                .addCapabilityBinding(OPEN_APP_FEATURE, FEATURE_PARAMETER, Arrays.asList(
                        context.getString(R.string.shortcut_label_wake, device.name),
                        context.getString(R.string.shortcut_voice_wake, device.name)))
                .setIcon(IconCompat.createWithResource(context, R.drawable.device_shortcut))
                .setRank(device.id * 2)
                .build();
    }

    public static ShortcutInfoCompat buildShutdownShortcut(Device device, Context context) {
        return new ShortcutInfoCompat.Builder(context, "shutdown_" + device.id)
                .setShortLabel(context.getString(R.string.shortcut_label_shutdown, device.name))
                .setLongLabel(context.getString(R.string.shortcut_label_shutdown, device.name))
                .setIntent(buildShutdownIntent(device, context))
                .addCapabilityBinding(OPEN_APP_FEATURE, FEATURE_PARAMETER, Arrays.asList(
                        context.getString(R.string.shortcut_label_shutdown, device.name),
                        context.getString(R.string.shortcut_voice_shutdown, device.name)))
                .setIcon(IconCompat.createWithResource(context, R.drawable.device_shortcut))
                .setRank(device.id * 2 + 1)
                .build();
    }

    @NonNull
    private static Intent buildWakeIntent(Device device, Context context) {
        Intent intent = new Intent(context, WakeDeviceActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        Bundle bundle = new Bundle();
        bundle.putInt(WakeDeviceActivity.DEVICE_ID_KEY, device.id);
        intent.putExtras(bundle);
        return intent;
    }

    @NonNull
    private static Intent buildShutdownIntent(Device device, Context context) {
        Intent intent = new Intent(context, ShutdownDeviceActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        Bundle bundle = new Bundle();
        bundle.putInt(ShutdownDeviceActivity.DEVICE_ID_KEY, device.id);
        intent.putExtras(bundle);
        return intent;
    }
}