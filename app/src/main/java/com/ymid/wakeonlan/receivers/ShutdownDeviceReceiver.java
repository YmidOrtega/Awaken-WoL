package com.ymid.wakeonlan.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.function.Consumer;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;

public class ShutdownDeviceReceiver extends BroadcastReceiver {

    public static final String ACTION_SHUTDOWN = "com.ymid.wakeonlan.ACTION_SHUTDOWN";
    public static final String EXTRA_DEVICE_ID = "deviceId";
    public static final String EXTRA_DEVICE_NAME = "deviceName";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_SHUTDOWN.equals(intent.getAction())) return;

        PendingResult pendingResult = goAsync();
        Consumer<Device> callback = device -> {
            try {
                if (device != null && device.remoteShutdownEnabled) {
                    AuthenticatedDeviceActionActivity.startShutdown(context, device.id, true);
                    Log.i("ShutdownDeviceReceiver", "Authentication requested for shutdown: " + device.name);
                } else {
                    Log.w("ShutdownDeviceReceiver", "Device not found or shutdown not enabled");
                }
            } catch (Exception e) {
                Log.e("ShutdownDeviceReceiver", "Error sending shutdown command", e);
            } finally {
                pendingResult.finish();
            }
        };

        DeviceRepository repo = DeviceRepository.getInstance(context);
        int deviceId = intent.getIntExtra(EXTRA_DEVICE_ID, -1);
        String deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);

        if (deviceId != -1) {
            repo.getByIdAsync(deviceId, callback);
        } else if (deviceName != null && !deviceName.isEmpty()) {
            repo.getByNameAsync(deviceName, callback);
        } else {
            Log.w("ShutdownDeviceReceiver", "Received ACTION_SHUTDOWN without deviceId or deviceName");
            pendingResult.finish();
        }
    }
}
