package com.ymid.wakeonlan.shutdown;

import android.content.Context;
import android.util.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.security.TofuHostKeyVerifier;
import com.ymid.wakeonlan.shutdown.listener.IgnoringShutdownExecutorListener;
import com.ymid.wakeonlan.shutdown.listener.ShutdownExecutorListener;

public class ShutdownExecutor {

    private static final Executor executor = Executors.newSingleThreadExecutor();

    static {
        // Override Android's BC implementation with official BC Provider
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    public static void shutdownDevice(Context context, Device device, ShutdownExecutorListener shutdownExecutorListener) {
        shutdownDevice(context, device, "linux", shutdownExecutorListener, false);
    }

    public static void shutdownDevice(Context context, Device device, String os, ShutdownExecutorListener shutdownExecutorListener) {
        shutdownDevice(context, device, os, shutdownExecutorListener, false);
    }

    public static void shutdownDevice(Context context, Device device, String os, ShutdownExecutorListener shutdownExecutorListener, boolean blockDangerousCommands) {
        Optional<ShutdownModel> optionalShutdownModel = ShutdownModelFactory.fromDevice(device);

        if (!optionalShutdownModel.isPresent()) {
            Log.w(ShutdownExecutor.class.getSimpleName(), "Can not shutdown device. Not all required fields were set");
            shutdownExecutorListener.onGeneralError(new IllegalArgumentException("Can not shutdown device. Not all required fields were set"), null);
            return;
        }

        ShutdownModel shutdownModel = optionalShutdownModel.get();
        ShutdownRunnable shutdownRunnable = new ShutdownRunnable(shutdownModel, shutdownExecutorListener, os,
                blockDangerousCommands, new TofuHostKeyVerifier(context));

        executor.execute(shutdownRunnable);
    }

    public static void shutdownDevice(Context context, Device device) {
        shutdownDevice(context, device, new IgnoringShutdownExecutorListener());
    }

    public static void shutdownDeviceForTest(Context context, Device device, ShutdownExecutorListener shutdownExecutorListener) {
        shutdownDevice(context, device, "linux", shutdownExecutorListener, true);
    }

    public static void shutdownDeviceForTest(Context context, Device device, String os, ShutdownExecutorListener shutdownExecutorListener) {
        shutdownDevice(context, device, os, shutdownExecutorListener, true);
    }
}
