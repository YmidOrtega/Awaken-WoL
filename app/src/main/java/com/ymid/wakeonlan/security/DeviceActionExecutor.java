package com.ymid.wakeonlan.security;

import android.content.Context;
import android.widget.Toast;

import com.google.common.base.Strings;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.entities.ActionType;
import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.ActionLogRepository;
import com.ymid.wakeonlan.shutdown.ShutdownExecutor;
import com.ymid.wakeonlan.shutdown.ShutdownModelFactory;
import com.ymid.wakeonlan.shutdown.listener.IgnoringShutdownExecutorListener;
import com.ymid.wakeonlan.ui.list.status.DeviceStatusBoard;
import com.ymid.wakeonlan.ui.notifications.NotificationHelper;
import com.ymid.wakeonlan.wol.WolSender;

public final class DeviceActionExecutor {

    private DeviceActionExecutor() {
    }

    public static boolean execute(Context context, Device device, String action) {
        if (AuthenticatedDeviceActionActivity.ACTION_SHUTDOWN.equals(action)) {
            return shutdown(context, device);
        }
        if (AuthenticatedDeviceActionActivity.ACTION_WAKE.equals(action)) {
            return wake(context, device);
        }
        return false;
    }

    private static boolean shutdown(Context context, Device device) {
        if (!ShutdownModelFactory.fromDevice(device).isPresent()) {
            return false;
        }
        ShutdownExecutor.shutdownDevice(context, device, device.shutdownOs == null ? "linux" : device.shutdownOs, new IgnoringShutdownExecutorListener());
        ActionLogRepository.getInstance(context).log(device.name, ActionType.SHUTDOWN);
        Toast.makeText(context, context.getString(R.string.remote_shutdown_send_command, device.name), Toast.LENGTH_LONG).show();
        NotificationHelper.INSTANCE.sendShutdownSentNotification(context, device.name);
        return true;
    }

    private static boolean wake(Context context, Device device) {
        if (Strings.isNullOrEmpty(device.macAddress)) {
            return false;
        }
        WolSender.sendWolPacket(device);
        DeviceStatusBoard.getInstance().markWakeSent(device.id);
        ActionLogRepository.getInstance(context).log(device.name, ActionType.WAKE);
        Toast.makeText(context, context.getString(R.string.wol_toast_sending_packet, device.name), Toast.LENGTH_LONG).show();
        NotificationHelper.INSTANCE.sendWakeSentNotification(context, device.name);
        return true;
    }
}
