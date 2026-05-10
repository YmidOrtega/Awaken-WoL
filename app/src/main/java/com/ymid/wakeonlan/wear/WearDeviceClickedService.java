package com.ymid.wakeonlan.wear;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;

public class WearDeviceClickedService extends WearableListenerService {

    private static final String DEVICE_CLICKED_PATH = "/device_clicked";

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(DEVICE_CLICKED_PATH)) {
            int deviceId = messageEvent.getData()[0];

            DeviceRepository deviceRepository = DeviceRepository.getInstance(this);
            Device device = deviceRepository.getById(deviceId);

            if (device != null) {
                AuthenticatedDeviceActionActivity.startWake(this, device.id);
            }
        }
    }
}
