package com.ymid.wakeonlan.ui.list.status;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTestItem;

public class PingDeviceStatusTesterBuilder implements DeviceStatusTesterBuilder {

    public Runnable buildStatusTestCallable(Device device, StatusTestItem statusTestItem) {
        return new PingRunnable(device, statusTestItem);
    }

}
