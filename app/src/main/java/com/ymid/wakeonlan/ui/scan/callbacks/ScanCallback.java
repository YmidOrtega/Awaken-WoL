package com.ymid.wakeonlan.ui.scan.callbacks;

public interface ScanCallback {

    void onError(int errorStringReference);

    void onDeviceFound(String ip, String hostName);

    void onTaskEnd();

    /** Called once per probed address, reachable or not, so the UI can show progress. */
    default void onAddressScanned() {
    }

}
