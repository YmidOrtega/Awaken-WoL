package com.ymid.wakeonlan.ui.scan.viewholder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Optional;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.ui.modify.AddNetworkScanDeviceActivity;
import com.ymid.wakeonlan.ui.scan.model.NetworkScanDevice;

public class ScanResultViewHolder extends RecyclerView.ViewHolder {

    private final TextView deviceName;
    private final TextView deviceIp;

    private final TextView addDevice;

    public ScanResultViewHolder(@NonNull View itemView) {
        super(itemView);

        deviceName = itemView.findViewById(R.id.scan_device_name);
        deviceIp = itemView.findViewById(R.id.scan_device_ip);
        addDevice = itemView.findViewById(R.id.scan_device_add);
    }

    /**
     * @param knownDeviceName name of the saved device at this address, or null when it is new
     */
    public void bind(NetworkScanDevice scanDevice, @Nullable String knownDeviceName) {
        Context context = itemView.getContext();
        Optional<String> hostName = scanDevice.getName();
        boolean known = knownDeviceName != null;

        deviceIp.setText(scanDevice.getIpAddress());
        deviceIp.setTextColor(ContextCompat.getColor(context, known ? R.color.primaryTextColorMuted : R.color.primaryTextColor));
        if (known) {
            deviceName.setText(context.getString(R.string.scan_already_added, knownDeviceName));
        } else {
            deviceName.setText(hostName.orElse(context.getString(R.string.scan_no_hostname)));
        }

        addDevice.setVisibility(known ? View.GONE : View.VISIBLE);
        addDevice.setOnClickListener(known ? null : view -> {
            Intent intent = new Intent(context, AddNetworkScanDeviceActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(AddNetworkScanDeviceActivity.MACHINE_NAME_KEY, hostName.orElse(null));
            bundle.putString(AddNetworkScanDeviceActivity.MACHINE_IP_KEY, scanDevice.getIpAddress());
            intent.putExtras(bundle);
            context.startActivity(intent);
        });
    }
}
