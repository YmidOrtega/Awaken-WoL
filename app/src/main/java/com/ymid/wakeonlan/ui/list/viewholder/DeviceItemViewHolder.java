package com.ymid.wakeonlan.ui.list.viewholder;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Strings;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.models.DeviceStatus;
import com.ymid.wakeonlan.shutdown.ShutdownModelFactory;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;
import com.ymid.wakeonlan.ui.LauncherIconManager;
import com.ymid.wakeonlan.ui.ObfuscatedTextView;
import com.ymid.wakeonlan.ui.list.DeviceClickedCallback;
import com.ymid.wakeonlan.ui.list.SparklineView;
import com.ymid.wakeonlan.ui.list.status.DeviceStatusBoard;
import com.ymid.wakeonlan.ui.list.status.DeviceStatusListener;
import com.ymid.wakeonlan.ui.list.status.DeviceStatusSnapshot;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTestType;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTesterPool;

public class DeviceItemViewHolder extends RecyclerView.ViewHolder {

    private enum RowState { UNKNOWN, ONLINE, OFFLINE, WAKING }

    private final View deviceStatus;
    private final TextView deviceName;
    private final ObfuscatedTextView deviceMacAddress;
    private final TextView deviceIp;
    private final TextView deviceStatusDetails;
    private final TextView deviceLatencyUnit;
    private final SparklineView latencySparkline;
    private final TextView wakeLog;

    private final TextView editButton;
    private final TextView sendWolButton;
    private final TextView shutdownButton;
    private final DeviceClickedCallback deviceClickedCallback;
    private final StatusTesterPool statusTesterPool;
    private final DeviceStatusBoard statusBoard = DeviceStatusBoard.getInstance();

    private Device device;
    private boolean drivesLauncherIcon;
    private boolean shutdownAvailable;
    private RowState shownState;

    public DeviceItemViewHolder(View view, DeviceClickedCallback deviceClickedCallback, StatusTesterPool statusTesterPool) {
        super(view);
        deviceStatus = view.findViewById(R.id.device_status);
        deviceName = view.findViewById(R.id.device_name);
        deviceMacAddress = view.findViewById(R.id.device_mac);
        deviceIp = view.findViewById(R.id.device_ip);
        deviceStatusDetails = view.findViewById(R.id.device_status_details);
        deviceLatencyUnit = view.findViewById(R.id.device_latency_unit);
        latencySparkline = view.findViewById(R.id.device_latency_sparkline);
        wakeLog = view.findViewById(R.id.device_wake_log);

        editButton = view.findViewById(R.id.edit);
        sendWolButton = view.findViewById(R.id.send_wol);
        shutdownButton = view.findViewById(R.id.shutdown);
        this.deviceClickedCallback = deviceClickedCallback;
        this.statusTesterPool = statusTesterPool;
    }

    public synchronized void fromDevice(Device device, boolean drivesLauncherIcon) {
        this.device = device;
        this.drivesLauncherIcon = drivesLauncherIcon;

        deviceName.setText(device.name);
        deviceMacAddress.setObfuscatedText(device.macAddress);
        // Without a status IP there is no meaningful address to show; the broadcast address would mislead
        deviceIp.setText(device.statusIp);
        deviceIp.setVisibility(Strings.isNullOrEmpty(device.statusIp) ? View.GONE : View.VISIBLE);

        setOnClickHandler(device);
        setOnEditClickHandler(device);
        setShutdownClickHandler(device);
        startDeviceStatusQuery(device);
    }

    public void setOnClickHandler(Device device) {
        sendWolButton.setOnClickListener(view -> {
            AuthenticatedDeviceActionActivity.startWake(view.getContext(), device.id);
            deviceClickedCallback.onDeviceClicked(deviceName.getText().toString());
        });
    }

    public void setOnEditClickHandler(Device device) {
        editButton.setOnClickListener(view -> {
            AuthenticatedDeviceActionActivity.startEdit(view.getContext(), device.id);
        });
    }

    public void setShutdownClickHandler(Device device) {
        shutdownAvailable = ShutdownModelFactory.fromDevice(device).isPresent();
        if (shutdownAvailable) {
            shutdownButton.setOnClickListener(v -> {
                AuthenticatedDeviceActionActivity.startShutdown(v.getContext(), device.id);
            });
        } else {
            shutdownButton.setOnClickListener(null);
        }
    }

    public void startDeviceStatusQuery(Device device) {
        shownState = null;
        render(statusBoard.isWaking(device.id) ? RowState.WAKING : RowState.UNKNOWN, null);

        final int deviceId = device.id;
        statusTesterPool.schedule(device, new DeviceStatusListener() {
            @Override
            public void onStatusAvailable(DeviceStatus deviceStatus) {
                updateStatusDetails(deviceId, new DeviceStatusSnapshot(deviceStatus, null));
            }

            @Override
            public void onStatusSnapshotAvailable(DeviceStatusSnapshot statusSnapshot) {
                updateStatusDetails(deviceId, statusSnapshot);
            }
        }, StatusTestType.LIST);
    }

    private void updateStatusDetails(int deviceId, DeviceStatusSnapshot statusSnapshot) {
        itemView.post(() -> {
            statusBoard.record(deviceId, statusSnapshot);
            // The row may have been rebound to another device while the check was running
            if (device == null || device.id != deviceId) {
                return;
            }

            DeviceStatus status = statusSnapshot.getStatus();
            if (status == DeviceStatus.ONLINE) {
                updateLauncherIconIfNeeded(true);
                render(RowState.ONLINE, statusSnapshot.getLatencyMs());
            } else if (status == DeviceStatus.OFFLINE) {
                updateLauncherIconIfNeeded(false);
                render(statusBoard.isWaking(deviceId) ? RowState.WAKING : RowState.OFFLINE, null);
            } else {
                render(RowState.UNKNOWN, null);
            }
        });
    }

    private void render(RowState state, Long latencyMs) {
        if (state == RowState.ONLINE) {
            deviceStatusDetails.setText(latencyMs == null ? "--" : String.valueOf(latencyMs));
            latencySparkline.setValues(statusBoard.getLatencyHistory(device.id));
        }
        if (state == shownState) {
            return;
        }
        shownState = state;

        int figureColor;
        int ledDrawable;
        switch (state) {
            case ONLINE:
                ledDrawable = R.drawable.device_status_online;
                figureColor = R.color.ledOnlineColor;
                break;
            case OFFLINE:
                ledDrawable = R.drawable.device_status_offline;
                figureColor = R.color.ledOfflineColor;
                deviceStatusDetails.setText(R.string.device_status_short_offline);
                break;
            case WAKING:
                ledDrawable = R.drawable.device_status_waking;
                figureColor = R.color.ledWakingColor;
                deviceStatusDetails.setText(R.string.device_status_short_waking);
                break;
            default:
                ledDrawable = R.drawable.device_status_unknown;
                figureColor = R.color.primaryTextColorMuted;
                deviceStatusDetails.setText(R.string.device_status_short_unknown);
                break;
        }

        deviceStatus.setBackground(AppCompatResources.getDrawable(itemView.getContext(), ledDrawable));
        deviceStatusDetails.setTextColor(ContextCompat.getColor(itemView.getContext(), figureColor));
        // Figures read large; status words stay smaller so they don't shout
        deviceStatusDetails.setTextSize(state == RowState.ONLINE ? 22 : 15);
        deviceLatencyUnit.setVisibility(state == RowState.ONLINE ? View.VISIBLE : View.GONE);

        latencySparkline.setColor(ContextCompat.getColor(itemView.getContext(),
                state == RowState.ONLINE ? R.color.ledOnlineColor : R.color.primaryTextColorMuted));
        if (state != RowState.ONLINE) {
            latencySparkline.setValues(statusBoard.getLatencyHistory(device.id));
        }

        // Offer only the actions that make sense for the current state
        boolean waking = state == RowState.WAKING;
        sendWolButton.setVisibility(state == RowState.OFFLINE || state == RowState.UNKNOWN ? View.VISIBLE : View.GONE);
        shutdownButton.setVisibility(shutdownAvailable && (state == RowState.ONLINE || state == RowState.UNKNOWN) ? View.VISIBLE : View.GONE);
        editButton.setVisibility(waking ? View.GONE : View.VISIBLE);
        wakeLog.setVisibility(waking ? View.VISIBLE : View.GONE);

        if (waking) {
            AlphaAnimation blink = new AlphaAnimation(1f, 0.2f);
            blink.setDuration(450);
            blink.setRepeatMode(Animation.REVERSE);
            blink.setRepeatCount(Animation.INFINITE);
            deviceStatus.startAnimation(blink);
        } else {
            deviceStatus.clearAnimation();
        }
    }

    private void updateLauncherIconIfNeeded(boolean online) {
        if (!drivesLauncherIcon) {
            return;
        }
        LauncherIconManager.updateLauncherIcon(itemView.getContext(), online);
    }

    public void cancelStatusUpdates() {
        if (statusTesterPool != null && device != null) {
            statusTesterPool.stopSingle(device, StatusTestType.LIST);
        }
    }
}
