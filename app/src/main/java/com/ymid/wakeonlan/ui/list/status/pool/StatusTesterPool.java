package com.ymid.wakeonlan.ui.list.status.pool;

import androidx.annotation.NonNull;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.ui.list.status.DeviceStatusListener;

public interface StatusTesterPool {

    void schedule(Device device, DeviceStatusListener deviceStatusListener, StatusTestType testType);

    void stopSingle(@NonNull Device device, StatusTestType testType);

    void stopAllForType(StatusTestType testType);

    void pauseAllForType(StatusTestType testType);

    void resumeAll();
}
