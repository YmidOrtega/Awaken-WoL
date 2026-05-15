package com.ymid.wakeonlan.wol;

import android.util.Log;

import com.google.common.base.Strings;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.ui.modify.BroadcastHelper;

public class WolSender {

    public static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    public static void sendWolPacket(Device device) {
        EXECUTOR.execute(() -> {
            // Send to the device's configured broadcast/IP address
            sendPacket(device, device.broadcastAddress);

            // Also send to the local network broadcast (auto-detected)
            new BroadcastHelper().getBroadcastAddress()
                    .ifPresent(addr -> sendPacket(device, addr.getHostAddress()));

            // If a WAN IP is configured, also send there (WOL over Internet via port forward)
            if (!Strings.isNullOrEmpty(device.wanIp)) {
                int wanPort = (device.wanPort != null && device.wanPort > 0) ? device.wanPort : device.port;
                sendPacketToAddress(device, device.wanIp, wanPort);
            }
        });
    }

    private static void sendPacket(Device device, String broadcastAddress) {
        if (Strings.isNullOrEmpty(broadcastAddress)) return;
        sendPacketToAddress(device, broadcastAddress, device.port);
    }

    private static void sendPacketToAddress(Device device, String address, int port) {
        if (Strings.isNullOrEmpty(address)) return;
        try {
            DatagramPacket packet = PacketBuilder.buildMagicPacket(address, device.macAddress, port, device.secureOnPassword);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
        } catch (Exception e) {
            Log.e(WolSender.class.getName(), "Error while sending magic packet to " + address, e);
        }
    }
}
