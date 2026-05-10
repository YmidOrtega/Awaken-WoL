package com.ymid.wakeonlan.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;

public class WakeDeviceReceiver extends BroadcastReceiver {

    public static final String ACTION_WAKE = "com.ymid.wakeonlan.ACTION_WAKE";
    public static final String EXTRA_DEVICE_ID = "deviceId";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_WAKE.equals(intent.getAction())) return;

        int deviceId = intent.getIntExtra(EXTRA_DEVICE_ID, -1);
        if (deviceId == -1) {
            Log.w("WakeDeviceReceiver", "Received ACTION_WAKE without deviceId");
            return;
        }

        try {
            Device device = DeviceRepository.getInstance(context).getById(deviceId);
            if (device != null && device.macAddress != null && !device.macAddress.isEmpty()) {
                AuthenticatedDeviceActionActivity.startWake(context, device.id);
                Log.i("WakeDeviceReceiver", "Authentication requested for WoL: " + device.name);
            } else {
                Log.w("WakeDeviceReceiver", "Device not found or missing MAC: id=" + deviceId);
            }
        } catch (Exception e) {
            Log.e("WakeDeviceReceiver", "Error sending WoL packet", e);
        }
    }
}
