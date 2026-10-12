package com.ymid.wakeonlan.ui.scan;

import android.util.Log;

import java.net.InetAddress;
import java.util.concurrent.Callable;

import com.ymid.wakeonlan.ui.scan.callbacks.ScanCallback;

final class ScanRunnable implements Callable<Void> {

    private final String ipPrefix;
    private final int beginIpInclusive;
    private final int endIp;
    private final ScanCallback scanCallback;

    ScanRunnable(String ipPrefix, int beginIpInclusive, int endIpInclusive, ScanCallback scanCallback) {
        this.ipPrefix = ipPrefix;
        this.beginIpInclusive = beginIpInclusive;
        this.endIp = endIpInclusive;
        this.scanCallback = scanCallback;
    }

    @Override
    public Void call() {
        for (int i = beginIpInclusive; i <= endIp; i++) {
            // One failing address must not skip the rest of the batch
            try {
                InetAddress address = InetAddress.getByName(ipPrefix + i);
                if (address.isReachable(400)) {
                    scanCallback.onDeviceFound(address.getHostAddress(), address.getHostName());
                }
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Error while scanning " + ipPrefix + i, e);
            }
        }

        return null;
    }
}
