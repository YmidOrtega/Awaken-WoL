package com.ymid.wakeonlan.ui.scan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.ui.scan.model.NetworkScanDevice;
import com.ymid.wakeonlan.ui.scan.viewholder.EmptyViewHolder;
import com.ymid.wakeonlan.ui.scan.viewholder.ListViewType;
import com.ymid.wakeonlan.ui.scan.viewholder.ScanResultViewHolder;

public class NetworkScanAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<NetworkScanDevice> deviceList = new ArrayList<>(0);
    private Map<String, String> knownDeviceNames = new HashMap<>();
    private boolean scanning;

    /** Saved devices by IP address, so results already in the app are marked instead of offered again. */
    public void setKnownDevices(List<Device> devices) {
        Map<String, String> names = new HashMap<>();
        for (Device device : devices) {
            for (String address : new String[]{device.statusIp, device.sshAddress, device.broadcastAddress}) {
                if (!Strings.isNullOrEmpty(address)) {
                    names.put(address, device.name);
                }
            }
        }
        knownDeviceNames = names;
        notifyDataSetChanged();
    }

    public void setScanning(boolean scanning) {
        this.scanning = scanning;
        if (deviceList.isEmpty()) {
            notifyItemChanged(0);
        }
    }

    public int getFoundCount() {
        return deviceList.size();
    }

    public void clearDataset() {
        deviceList = new ArrayList<>();
        updateDeviceList();
    }

    public synchronized void addDevice(NetworkScanDevice networkScanDevice) {
        deviceList.add(networkScanDevice);
        updateDeviceList();
    }

    private void updateDeviceList() {
        List<NetworkScanDevice> sortedList = deviceList.stream()
                .distinct()
                .sorted(getScanDeviceComparator())
                .collect(Collectors.toList());

        NetworkScanDiffCallback diffCallback = new NetworkScanDiffCallback(Collections.unmodifiableList(deviceList), sortedList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.deviceList = sortedList;

        diffResult.dispatchUpdatesTo(this);
    }

    private Comparator<NetworkScanDevice> getScanDeviceComparator() {
        return Comparator.comparingInt(device -> Integer.parseInt(getLastIpOctet(device)));
    }

    @NonNull
    private String getLastIpOctet(NetworkScanDevice device) {
        return device.getIpAddress()
                .substring(device.getIpAddress()
                        .lastIndexOf(".") + 1);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        RecyclerView.ViewHolder viewHolder;

        if (ListViewType.EMPTY.ordinal() == viewType) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.network_list_empty, parent, false);
            viewHolder = new EmptyViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.network_list_item, parent, false);
            viewHolder = new ScanResultViewHolder(view);
        }

        return viewHolder;
    }

    @Override
    public int getItemViewType(int position) {
        if (deviceList.isEmpty()) {
            return ListViewType.EMPTY.ordinal();
        } else {
            return ListViewType.SCAN_DEVICE.ordinal();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (getItemViewType(position) == ListViewType.SCAN_DEVICE.ordinal()) {
            ScanResultViewHolder scanResultViewHolder = (ScanResultViewHolder) viewHolder;

            NetworkScanDevice networkScanDevice = deviceList.get(position);

            scanResultViewHolder.bind(networkScanDevice, knownDeviceNames.get(networkScanDevice.getIpAddress()));
        } else {
            ((EmptyViewHolder) viewHolder).bind(scanning);
        }
    }

    @Override
    public long getItemId(int position) {
        if (deviceList.isEmpty()) {
            return RecyclerView.NO_ID;
        }
        return deviceList.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        if (deviceList.isEmpty()) {
            return 1; // "Empty" item
        }
        return deviceList.size();
    }
}
