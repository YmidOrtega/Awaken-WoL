package com.ymid.wakeonlan.appactions;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Strings;

import java.util.List;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;
import com.ymid.wakeonlan.ui.MainActivity;
import com.ymid.wakeonlan.ui.list.ConsoleCommand;

public class DeepLinkHandlerActivity extends AppCompatActivity {

    /**
     * wakeonlan://action/feature?name=… from the Assistant's OPEN_APP_FEATURE capability:
     * a shortcut id (see AssistantFeature) or, failing that, a phrase like "Apaga Ubuntu".
     */
    private static final String FEATURE_ACTION = "feature";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data == null) {
            finish();
            return;
        }

        String path = data.getPath();
        String action = null;
        if (path != null && path.length() > 1) {
            action = path.substring(1);
        }

        String name = data.getQueryParameter("name");
        if (Strings.isNullOrEmpty(action) || Strings.isNullOrEmpty(name)) {
            Toast.makeText(this, "Invalid deep link", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String resolvedAction = action;
        DeviceRepository.getInstance(this).getAllAsync(devices ->
                handleAction(resolvedAction, name, devices));
    }

    private void handleAction(String action, String name, List<Device> devices) {
        if (FEATURE_ACTION.equalsIgnoreCase(action)) {
            handleAssistantFeature(name, devices);
            return;
        }

        Device matched = null;
        for (Device d : devices) {
            if (d != null && d.name != null && d.name.equalsIgnoreCase(name)) {
                matched = d;
                break;
            }
        }

        if (matched == null) {
            Toast.makeText(this, "Device not found: " + name, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if ("wake".equalsIgnoreCase(action)) {
            AuthenticatedDeviceActionActivity.startWake(this, matched.id, true);
        } else if ("shutdown".equalsIgnoreCase(action)) {
            AuthenticatedDeviceActionActivity.startShutdown(this, matched.id, true);
        } else {
            Toast.makeText(this, "Unknown action: " + action, Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private void handleAssistantFeature(String value, List<Device> devices) {
        AssistantFeature feature = AssistantFeature.parse(value);
        switch (feature.kind) {
            case WAKE_DEVICE:
            case SHUTDOWN_DEVICE:
                Device device = findById(devices, feature.deviceId);
                if (device == null) {
                    Toast.makeText(this, getString(R.string.command_device_not_found, value), Toast.LENGTH_LONG).show();
                } else if (feature.kind == AssistantFeature.Kind.WAKE_DEVICE) {
                    AuthenticatedDeviceActionActivity.startWake(this, device.id, true);
                } else {
                    AuthenticatedDeviceActionActivity.startShutdown(this, device.id, true);
                }
                break;
            case OPEN_SCAN:
                startActivity(new Intent(this, MainActivity.class)
                        .putExtra(MainActivity.EXTRA_OPEN_SCAN, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                break;
            default:
                handleSpokenCommand(feature.value, devices);
                return;
        }
        finish();
    }

    @Nullable
    private static Device findById(List<Device> devices, int id) {
        for (Device device : devices) {
            if (device != null && device.id == id) {
                return device;
            }
        }
        return null;
    }

    private void handleSpokenCommand(String phrase, List<Device> devices) {
        ConsoleCommand command = ConsoleCommand.parse(phrase, devices);
        if (command.error == ConsoleCommand.Error.DEVICE_NOT_FOUND) {
            Toast.makeText(this, getString(R.string.command_device_not_found, command.query), Toast.LENGTH_LONG).show();
        } else if (command.device != null && command.action == ConsoleCommand.Action.WAKE) {
            AuthenticatedDeviceActionActivity.startWake(this, command.device.id, true);
        } else if (command.device != null && command.action == ConsoleCommand.Action.SHUTDOWN) {
            AuthenticatedDeviceActionActivity.startShutdown(this, command.device.id, true);
        } else {
            Toast.makeText(this, R.string.command_unknown, Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
