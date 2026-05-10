package com.ymid.wakeonlan.ui.list.status;

import android.util.Log;

import java.net.InetAddress;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.models.DeviceStatus;
import com.ymid.wakeonlan.ping.Ping;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTestItem;

public class PingRunnable implements Runnable {

    private final Device device;
    private final StatusTestItem statusTestItem;

    private boolean skipRunningExecutionResults = false;

    public PingRunnable(Device device, StatusTestItem statusTestItem) {
        this.device = device;
        this.statusTestItem = statusTestItem;
    }

    public void cancelStatusUpdates() {
        skipRunningExecutionResults = true;
    }

    @Override
    public void run() {
        String ipToPing = device.statusIp;
        if (ipToPing == null || ipToPing.isEmpty()) {
            ipToPing = device.sshAddress;
        }

        if (ipToPing == null || ipToPing.isEmpty()) {
            notifyDeviceStatusListeners(new DeviceStatusSnapshot(DeviceStatus.UNKNOWN, null));
            return;
        }

        try {
            final InetAddress dest = InetAddress.getByName(ipToPing);
            final Ping ping = new Ping(dest, new Ping.PingListener() {
                @Override
                public void onPing(final long timeMs) {
                    if (timeMs == -1L) {
                        Log.w(getClass().getSimpleName(), String.format("Ping timed out for IP %s", device.statusIp));
                        notifyDeviceStatusListeners(new DeviceStatusSnapshot(DeviceStatus.OFFLINE, null));
                        return;
                    }
                    notifyDeviceStatusListeners(new DeviceStatusSnapshot(DeviceStatus.ONLINE, timeMs));
                }

                @Override
                public void onPingException(final Exception e) {
                    Log.w(getClass().getSimpleName(), String.format("Error while pinging device with IP %s", device.statusIp), e);
                    notifyDeviceStatusListeners(new DeviceStatusSnapshot(DeviceStatus.OFFLINE, null));
                }
            });
            ping.setTimeoutMs(1000);
            ping.run();
        } catch (Throwable t) {
            Log.w(getClass().getSimpleName(), String.format("Critical error while pinging device with IP %s", ipToPing), t);
            notifyDeviceStatusListeners(new DeviceStatusSnapshot(DeviceStatus.UNKNOWN, null));
        }
    }

    private void notifyDeviceStatusListeners(DeviceStatusSnapshot statusSnapshot) {
        if (!skipRunningExecutionResults) {
            statusTestItem.forAllListeners(deviceStatusListener -> deviceStatusListener.onStatusSnapshotAvailable(statusSnapshot));
        }
    }

}
