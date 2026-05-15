package com.ymid.wakeonlan.ui.modify;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.common.base.Strings;

import java.util.Objects;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.models.Device;

public class EditDeviceActivity extends ModifyDeviceActivity {

    public static final String DEVICE_PARCELABLE_KEY = "machine";
    private Device device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        populateInputs();
    }

    private void populateInputs() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            device = extras.getParcelable(DEVICE_PARCELABLE_KEY);
            if (device == null) {
                Toast.makeText(this, R.string.edit_machine_error_loading, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            deviceNameInput.setText(device.name);
            deviceGroupInput.setText(com.google.common.base.Strings.nullToEmpty(device.groupName));
            deviceStatusIpInput.setRealText(device.statusIp);
            deviceMacInput.setRealText(device.macAddress);
            deviceBroadcastInput.setRealText(device.broadcastAddress);
            devicePorts.setText(String.valueOf(device.port));
            deviceWanAddressInput.setRealText(device.wanIp);
            deviceWanPortInput.setText(device.wanPort == null ? "" : String.valueOf(device.wanPort));
            deviceSecureOnPassword.setText(device.secureOnPassword);

            deviceEnableRemoteShutdown.setChecked(device.remoteShutdownEnabled);
            triggerRemoteShutdownLayoutVisibility(device.remoteShutdownEnabled);
            deviceSshAddressInput.setRealText(device.sshAddress);
            deviceSshPortInput.setText(getSshPortFallback());
            deviceSshUsernameInput.setText(device.sshUsername);
            deviceSshPasswordInput.setText(device.sshPassword);
            deviceSshCommandInput.setText(device.sshCommand);
            sshKeyAlias = device.sshKeyAlias;
            setSshAuthType(device.sshAuthType);
            updateSshAuthUi();
            // set spinner to saved OS
            if (device.shutdownOs != null) {
                if (deviceSshOsSpinner.getAdapter() != null) {
                    int count = deviceSshOsSpinner.getAdapter().getCount();
                    for (int i = 0; i < count; i++) {
                        Object item = deviceSshOsSpinner.getAdapter().getItem(i);
                        if (item != null && item.toString().equalsIgnoreCase(device.shutdownOs)) {
                            deviceSshOsSpinner.setSelection(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    @NonNull
    private String getSshPortFallback() {
        return device.sshPort == null || device.sshPort < 0 ? "" : String.valueOf(device.sshPort);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_device_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.edit_machine_menu_save) {
            checkAndPersistDevice();
            return true;
        } else if (item.getItemId() == R.id.edit_machine_menu_delete) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.edit_device_delete_title)
                    .setMessage(R.string.edit_device_delete_message)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        deviceRepository.delete(device);
                        finish();
                    })
                    .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    })
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean inputsHaveNotChanged() {
        return Strings.nullToEmpty(device.name).equals(getDeviceNameInputText()) &&
                Strings.nullToEmpty(device.broadcastAddress).equals(getDeviceBroadcastAddressText()) &&
                Strings.nullToEmpty(device.macAddress).equals(getDeviceMacInputText()) &&
                Strings.nullToEmpty(device.statusIp).equals(getDeviceStatusIpText()) &&
                Strings.nullToEmpty(device.wanIp).equals(getDeviceWanAddressText()) &&
                Objects.equals(device.wanPort, getDeviceWanPort()) &&
                device.port == getPort() &&
                Strings.nullToEmpty(device.secureOnPassword).equals(getDeviceSecureOnPassword()) &&
                device.remoteShutdownEnabled == getDeviceRemoteShutdownEnabled() &&
                Strings.nullToEmpty(device.sshAddress).equals(getDeviceSshAddress()) &&
                Objects.equals(device.sshPort == null ? -1 : device.sshPort, getDeviceSshPort()) &&
                Strings.nullToEmpty(device.sshUsername).equals(getDeviceSshUsername()) &&
                Strings.nullToEmpty(device.sshPassword).equals(getDeviceSshPassword()) &&
                Strings.nullToEmpty(device.sshCommand).equals(getDeviceSshCommand()) &&
                Strings.nullToEmpty(device.sshAuthType == null ? "password" : device.sshAuthType).equals(getDeviceSshAuthType()) &&
                Strings.nullToEmpty(device.sshKeyAlias).equals(Strings.nullToEmpty(getDeviceSshKeyAlias())) &&
                Strings.nullToEmpty(device.groupName).equals(getDeviceGroupInputText());

    }

    @Override
    protected void persistDevice(Device device) {
        deviceRepository.update(device);
    }

    @Override
    protected Device buildDeviceFromInputs() {
        device.name = getDeviceNameInputText();
        device.statusIp = getDeviceStatusIpText();
        device.macAddress = getDeviceMacInputText();
        device.broadcastAddress = getDeviceBroadcastAddressText();
        device.port = getPort();
        device.wanIp = getDeviceWanAddressText();
        device.wanPort = getDeviceWanPort();
        device.secureOnPassword = getDeviceSecureOnPassword();
        device.remoteShutdownEnabled = getDeviceRemoteShutdownEnabled();
        device.sshAddress = getDeviceSshAddress();
        device.sshPort = getDeviceSshPort();
        device.sshUsername = getDeviceSshUsername();
        device.sshPassword = getDeviceSshPassword();
        device.sshCommand = getDeviceSshCommand();
        device.shutdownOs = getSelectedOs(deviceSshOsSpinner);
        device.sshAuthType = getDeviceSshAuthType();
        device.sshKeyAlias = getDeviceSshKeyAlias();
        String group = getDeviceGroupInputText();
        device.groupName = group.isEmpty() ? null : group;

        return device;
    }

}
