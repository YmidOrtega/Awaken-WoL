package com.ymid.wakeonlan.ui.list.viewholder;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.models.DeviceStatus;
import com.ymid.wakeonlan.shutdown.ShutdownModelFactory;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;
import com.ymid.wakeonlan.ui.LauncherIconManager;
import com.ymid.wakeonlan.ui.ObfuscatedTextView;
import com.ymid.wakeonlan.ui.list.DeviceClickedCallback;
import com.ymid.wakeonlan.ui.list.status.DeviceStatusListener;
import com.ymid.wakeonlan.ui.list.status.DeviceStatusSnapshot;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTestType;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTesterPool;

public class DeviceItemViewHolder extends RecyclerView.ViewHolder {

    private final View deviceStatus;
    private final TextView deviceName;
    private final ObfuscatedTextView deviceMacAddress;
    private final TextView deviceStatusDetails;

    private final Button editButton;
    private final Button sendWolButton;
    private final Button shutdownButton;
    private final DeviceClickedCallback deviceClickedCallback;
    private final StatusTesterPool statusTesterPool;

    private Device device;

    public DeviceItemViewHolder(View view, DeviceClickedCallback deviceClickedCallback, StatusTesterPool statusTesterPool) {
        super(view);
        deviceStatus = view.findViewById(R.id.device_status);
        deviceName = view.findViewById(R.id.device_name);
        deviceMacAddress = view.findViewById(R.id.device_mac);
        deviceStatusDetails = view.findViewById(R.id.device_status_details);

        editButton = view.findViewById(R.id.edit);
        sendWolButton = view.findViewById(R.id.send_wol);
        shutdownButton = view.findViewById(R.id.shutdown);
        this.deviceClickedCallback = deviceClickedCallback;
        this.statusTesterPool = statusTesterPool;
    }

    public synchronized void fromDevice(Device device) {
        this.device = device;

        deviceName.setText(device.name);
        deviceMacAddress.setObfuscatedText(device.macAddress);

        setOnClickHandler(device);
        setOnEditClickHandler(device);
        setShutdownVisibilityAndClickHandler(device);
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

    public void setShutdownVisibilityAndClickHandler(Device device) {
        boolean shutdownConfigurationValid = ShutdownModelFactory.fromDevice(device).isPresent();

        shutdownButton.setVisibility(shutdownConfigurationValid ? View.VISIBLE : View.GONE);

        if (shutdownConfigurationValid) {
            shutdownButton.setOnClickListener(v -> {
                AuthenticatedDeviceActionActivity.startShutdown(v.getContext(), device.id);
            });
        }
    }

    public void startDeviceStatusQuery(Device device) {
        deviceStatus.clearAnimation();
        deviceStatus.setBackground(AppCompatResources.getDrawable(itemView.getContext(), R.drawable.device_status_unknown));
        deviceStatusDetails.setText(R.string.device_status_unknown);

        statusTesterPool.schedule(device, new DeviceStatusListener() {
            @Override
            public void onStatusAvailable(DeviceStatus deviceStatus) {
                updateStatusDetails(new DeviceStatusSnapshot(deviceStatus, null));
            }

            @Override
            public void onStatusSnapshotAvailable(DeviceStatusSnapshot statusSnapshot) {
                updateStatusDetails(statusSnapshot);
            }
        }, StatusTestType.LIST);
    }

    private void updateStatusDetails(DeviceStatusSnapshot statusSnapshot) {
        itemView.post(() -> {
            DeviceStatus status = statusSnapshot.getStatus();
            if (status == DeviceStatus.ONLINE) {
                updateLauncherIconIfNeeded(true);
                setAlphaAnimationIfNotSet();
                setStatusDrawable(R.drawable.device_status_online);
                setOnlineStatusDetails(statusSnapshot);
            } else if (status == DeviceStatus.OFFLINE) {
                updateLauncherIconIfNeeded(false);
                setAlphaAnimationIfNotSet();
                setStatusDrawable(R.drawable.device_status_offline);
                deviceStatusDetails.setText(R.string.device_status_offline);
            } else {
                deviceStatus.clearAnimation();
                deviceStatus.setBackground(AppCompatResources.getDrawable(itemView.getContext(), R.drawable.device_status_unknown));
                deviceStatusDetails.setText(R.string.device_status_unknown);
            }
        });
    }

    private void updateLauncherIconIfNeeded(boolean online) {
        int position = getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION || position != 0) {
            return;
        }
        LauncherIconManager.updateLauncherIcon(itemView.getContext(), online);
    }

    private void setOnlineStatusDetails(DeviceStatusSnapshot statusSnapshot) {
        Long latencyMs = statusSnapshot.getLatencyMs();
        if (latencyMs == null) {
            deviceStatusDetails.setText(R.string.device_status_online_without_latency);
            return;
        }

        deviceStatusDetails.setText(itemView.getContext().getString(R.string.device_status_online_with_latency, latencyMs));
    }

    private void setStatusDrawable(int statusDrawable) {
        Drawable[] drawables = {
                deviceStatus.getBackground(),
                AppCompatResources.getDrawable(itemView.getContext(), statusDrawable)
        };

        TransitionDrawable transitionDrawable = new TransitionDrawable(drawables);
        deviceStatus.setBackground(transitionDrawable);
        transitionDrawable.startTransition(600);
    }

    private void setAlphaAnimationIfNotSet() {
        if (deviceStatus.getAnimation() == null) {
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.4f);
            alphaAnimation.setDuration(1500);
            alphaAnimation.setRepeatCount(Animation.INFINITE);
            alphaAnimation.setInterpolator(new AccelerateInterpolator());
            alphaAnimation.setRepeatMode(Animation.REVERSE);
            deviceStatus.startAnimation(alphaAnimation);
        }
    }

    public void cancelStatusUpdates() {
        if (statusTesterPool != null && device != null) {
            statusTesterPool.stopSingle(device, StatusTestType.LIST);
        }
    }
}
