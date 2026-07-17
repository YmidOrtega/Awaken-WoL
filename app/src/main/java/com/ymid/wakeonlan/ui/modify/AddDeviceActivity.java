package com.ymid.wakeonlan.ui.modify;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.models.Device;

public class AddDeviceActivity extends ModifyDeviceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        triggerRemoteShutdownLayoutVisibility(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_device_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.add_device_menu_save) {
            checkAndPersistDevice();
            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void persistDevice(Device device) {
        deviceRepository.insertAll(device);
    }

    @Override
    protected Device buildDeviceFromInputs() {
        Device device = new Device();
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

    @Override
    protected boolean inputsHaveNotChanged() {
        return getDeviceNameInputText().isEmpty() && getDeviceMacInputText().isEmpty()
                && getPort() == 9
                && getDeviceBroadcastAddressText().isEmpty() && getDeviceStatusIpText().isEmpty()
                && getDeviceWanAddressText().isEmpty() && getDeviceWanPort() == null
                && getDeviceSecureOnPassword().isEmpty() && !getDeviceRemoteShutdownEnabled() &&
                getDeviceSshAddress().isEmpty() && getDeviceSshPort() == -1 && getDeviceSshUsername().isEmpty() &&
                getDeviceSshPassword().isEmpty() && getDeviceSshCommand().isEmpty() &&
                "password".equals(getDeviceSshAuthType()) && getDeviceSshKeyAlias() == null
                && getDeviceGroupInputText().isEmpty();
    }
}
