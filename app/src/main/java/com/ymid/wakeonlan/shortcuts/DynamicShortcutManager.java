package com.ymid.wakeonlan.shortcuts;

import android.content.Context;
import android.os.Build;

import androidx.core.content.pm.ShortcutManagerCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ymid.wakeonlan.persistence.models.Device;

public class DynamicShortcutManager {

    // Android permite máximo 4 dynamic shortcuts
    public static final int SHORTCUT_AMOUNT_LIMIT = 4;

    public void updateShortcuts(Context context, List<Device> devices) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return;
        }

        removeOldShortcuts(context);
        publishShortcuts(context, devices);
    }

    private void publishShortcuts(Context context, List<Device> devices) {
        List<com.ymid.wakeonlan.persistence.models.Device> sorted = new ArrayList<>(devices);
        sorted.sort(Comparator.comparingInt((Device d) -> d.id));

        int published = 0;
        for (Device device : sorted) {
            if (published >= SHORTCUT_AMOUNT_LIMIT) break;

            // Siempre publicar el shortcut de WoL
            ShortcutManagerCompat.pushDynamicShortcut(context,
                    DeviceShortcutMapper.buildWakeShortcut(device, context));
            published++;

            // Publicar el shortcut de shutdown solo si el device lo tiene configurado
            if (device.remoteShutdownEnabled && published < SHORTCUT_AMOUNT_LIMIT) {
                ShortcutManagerCompat.pushDynamicShortcut(context,
                        DeviceShortcutMapper.buildShutdownShortcut(device, context));
                published++;
            }
        }
    }

    private void removeOldShortcuts(Context context) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context);
    }
}