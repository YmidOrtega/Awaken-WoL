package com.ymid.wakeonlan.ui.list.status;

import com.ymid.wakeonlan.persistence.models.DeviceStatus;

public interface DeviceStatusListener {

    void onStatusAvailable(DeviceStatus deviceStatus);

    default void onStatusSnapshotAvailable(DeviceStatusSnapshot statusSnapshot) {
        onStatusAvailable(statusSnapshot.getStatus());
    }

}
