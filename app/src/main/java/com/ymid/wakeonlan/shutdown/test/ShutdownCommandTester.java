package com.ymid.wakeonlan.shutdown.test;

import android.content.Context;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.shutdown.ShutdownExecutor;
import com.ymid.wakeonlan.shutdown.listener.ShutdownExecutorListener;

public class ShutdownCommandTester {

    private final Context context;
    private final ShutdownExecutorListener shutdownExecutorListener;

    public ShutdownCommandTester(Context context, ShutdownExecutorListener shutdownExecutorListener) {
        this.context = context;
        this.shutdownExecutorListener = shutdownExecutorListener;
    }

    public void startShutdownCommandTest(Device device) {
        // default to linux
        ShutdownExecutor.shutdownDeviceForTest(context, device, shutdownExecutorListener);
    }

    public void startShutdownCommandTest(Device device, String os) {
        ShutdownExecutor.shutdownDeviceForTest(context, device, os, shutdownExecutorListener);
    }

}
