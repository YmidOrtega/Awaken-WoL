package com.ymid.wakeonlan.ui.list.status;

import androidx.annotation.Nullable;

import com.ymid.wakeonlan.persistence.models.DeviceStatus;

public class DeviceStatusSnapshot {

    private final DeviceStatus status;
    @Nullable
    private final Long latencyMs;
    private final long measuredAtMs;

    public DeviceStatusSnapshot(DeviceStatus status, @Nullable Long latencyMs) {
        this.status = status;
        this.latencyMs = latencyMs;
        this.measuredAtMs = System.currentTimeMillis();
    }

    public DeviceStatus getStatus() {
        return status;
    }

    @Nullable
    public Long getLatencyMs() {
        return latencyMs;
    }

    public long getMeasuredAtMs() {
        return measuredAtMs;
    }
}
