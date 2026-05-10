package com.ymid.wakeonlan.shortcuts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.models.Device;

public class DeviceShortcutMapper {

    // Shortcut de encendido (WoL) — igual que antes
    public static ShortcutInfoCompat buildWakeShortcut(Device device, Context context) {
        return new ShortcutInfoCompat.Builder(context, "wake_" + device.id)
                .setShortLabel(device.name)
                .setLongLabel(device.name + " – " + context.getString(R.string.shortcut_action_wake))
                .setIntent(buildWakeIntent(device, context))
                .setIcon(IconCompat.createWithResource(context, R.drawable.device_shortcut))
                .setRank(device.id * 2)
                .build();
    }

    // Shortcut de apagado (SSH) — nuevo
    public static ShortcutInfoCompat buildShutdownShortcut(Device device, Context context) {
        return new ShortcutInfoCompat.Builder(context, "shutdown_" + device.id)
                .setShortLabel(device.name + " ↓")
                .setLongLabel(device.name + " – " + context.getString(R.string.shortcut_action_shutdown))
                .setIntent(buildShutdownIntent(device, context))
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