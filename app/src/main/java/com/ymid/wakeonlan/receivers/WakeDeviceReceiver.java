package com.ymid.wakeonlan.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.function.Consumer;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;

public class WakeDeviceReceiver extends BroadcastReceiver {

    public static final String ACTION_WAKE = "com.ymid.wakeonlan.ACTION_WAKE";
    public static final String EXTRA_DEVICE_ID = "deviceId";
    public static final String EXTRA_DEVICE_NAME = "deviceName";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_WAKE.equals(intent.getAction())) return;

        PendingResult pendingResult = goAsync();
        Consumer<Device> callback = device -> {
            try {
                if (device != null && device.macAddress != null && !device.macAddress.isEmpty()) {
                    AuthenticatedDeviceActionActivity.requestFromBackground(context, device, AuthenticatedDeviceActionActivity.ACTION_WAKE);
                    Log.i("WakeDeviceReceiver", "WoL requested: " + device.name);
                } else {
                    Log.w("WakeDeviceReceiver", "Device not found or missing MAC");
                }
            } catch (Exception e) {
                Log.e("WakeDeviceReceiver", "Error sending WoL packet", e);
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
            Log.w("WakeDeviceReceiver", "Received ACTION_WAKE without deviceId or deviceName");
            pendingResult.finish();
        }
    }
}
