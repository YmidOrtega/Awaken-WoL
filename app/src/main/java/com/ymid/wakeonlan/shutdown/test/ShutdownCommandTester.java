package com.ymid.wakeonlan.shutdown.test;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.shutdown.ShutdownExecutor;
import com.ymid.wakeonlan.shutdown.listener.ShutdownExecutorListener;

public class ShutdownCommandTester {

    private final ShutdownExecutorListener shutdownExecutorListener;

    public ShutdownCommandTester(ShutdownExecutorListener shutdownExecutorListener) {
        this.shutdownExecutorListener = shutdownExecutorListener;
    }

    public void startShutdownCommandTest(Device device) {
        // default to linux
        ShutdownExecutor.shutdownDeviceForTest(device, shutdownExecutorListener);
    }

    public void startShutdownCommandTest(Device device, String os) {
        ShutdownExecutor.shutdownDeviceForTest(device, os, shutdownExecutorListener);
    }

}
