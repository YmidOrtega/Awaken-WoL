package com.ymid.wakeonlan.shutdown;

import androidx.annotation.Nullable;

import com.google.common.base.Strings;

import java.util.Optional;

import com.ymid.wakeonlan.persistence.models.Device;

public class ShutdownModelFactory {

    private static final int DEFAULT_SSH_PORT = 22;

    public static Optional<ShutdownModel> fromDevice(Device device) {
        boolean shutdownEnabled = device.remoteShutdownEnabled;
        String address = getValueOrFallback(device.sshAddress, device.statusIp);
        int port = getSshPortOrFallback(device.sshPort);
        String username = getValueOrFallback(device.sshUsername, null);
        String password = getValueOrFallback(device.sshPassword, null);
        String command = getValueOrFallback(device.sshCommand, null);
        String authType = device.sshAuthType == null ? "password" : device.sshAuthType;
        String keyAlias = device.sshKeyAlias;

        boolean keyAuth = "key".equalsIgnoreCase(authType);

        if (allRequiredFieldsSet(shutdownEnabled, address, username, password, command, keyAuth, keyAlias)) {
            return Optional.of(new ShutdownModel(address, port, username, password, command, authType, keyAlias));
        }

        return Optional.empty();
    }

    private static boolean allRequiredFieldsSet(boolean shutdownEnabled, String address, String username,
                                                 String password, String command, boolean keyAuth, String keyAlias) {
        if (!shutdownEnabled || address == null || username == null || command == null) return false;
        if (keyAuth) {
            return keyAlias != null && !keyAlias.isEmpty();
        }
        return password != null;
    }

    @Nullable
    private static String getValueOrFallback(@Nullable String value, @Nullable String fallback) {
        if (!Strings.isNullOrEmpty(value)) return value;
        if (!Strings.isNullOrEmpty(fallback)) return fallback;
        return null;
    }

    private static Integer getSshPortOrFallback(@Nullable Integer value) {
        if (value != null && value > 0) return value;
        return DEFAULT_SSH_PORT;
    }
}
