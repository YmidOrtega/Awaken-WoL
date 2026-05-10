package com.ymid.wakeonlan.ui.list.status;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTestItem;

public interface DeviceStatusTesterBuilder {

    Runnable buildStatusTestCallable(Device device, StatusTestItem statusTestItem);
}
