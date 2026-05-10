package com.ymid.wakeonlan.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;

public class ShutdownDeviceReceiver extends BroadcastReceiver {

    public static final String ACTION_SHUTDOWN = "com.ymid.wakeonlan.ACTION_SHUTDOWN";
    public static final String EXTRA_DEVICE_ID = "deviceId";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_SHUTDOWN.equals(intent.getAction())) return;

        int deviceId = intent.getIntExtra(EXTRA_DEVICE_ID, -1);
        if (deviceId == -1) {
            Log.w("ShutdownDeviceReceiver", "Received ACTION_SHUTDOWN without deviceId");
            return;
        }

        try {
            Device device = DeviceRepository.getInstance(context).getById(deviceId);
            if (device != null && device.remoteShutdownEnabled) {
                AuthenticatedDeviceActionActivity.startShutdown(context, device.id);
                Log.i("ShutdownDeviceReceiver", "Authentication requested for shutdown: " + device.name);
            } else {
                Log.w("ShutdownDeviceReceiver", "Device not found or shutdown not enabled: id=" + deviceId);
            }
        } catch (Exception e) {
            Log.e("ShutdownDeviceReceiver", "Error sending shutdown command", e);
        }
    }
}
