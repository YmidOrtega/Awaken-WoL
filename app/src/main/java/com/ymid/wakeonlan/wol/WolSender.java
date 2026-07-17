package com.ymid.wakeonlan.wol;

import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.ui.modify.BroadcastHelper;

public class WolSender {

    public static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    private static final List<String> WOL_IFACE_PREFIXES = Lists.newArrayList("wlan", "eth", "tun");
    private static final String IPV6_ALL_NODES = "ff02::1";

    public static void sendWolPacket(Device device) {
        EXECUTOR.execute(() -> {
            sendPacket(device, device.broadcastAddress);
            new BroadcastHelper().getBroadcastAddress()
                    .ifPresent(addr -> sendPacket(device, addr.getHostAddress()));

            if (!IPV6_ALL_NODES.equalsIgnoreCase(PacketBuilder.normalizeAddress(device.broadcastAddress))) {
                sendIpv6MulticastFallback(device);
            }

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
            if (packet.getAddress() instanceof Inet6Address && packet.getAddress().isMulticastAddress()) {
                sendMulticastOnAllInterfaces(packet);
            } else {
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.send(packet);
                }
            }
        } catch (Exception e) {
            Log.e(WolSender.class.getName(), "Error while sending magic packet to " + address, e);
        }
    }

    private static void sendMulticastOnAllInterfaces(DatagramPacket packet) {
        for (NetworkInterface iface : getWolNetworkInterfaces()) {
            try (MulticastSocket socket = new MulticastSocket()) {
                socket.setNetworkInterface(iface);
                socket.send(packet);
            } catch (IOException e) {
                Log.w(WolSender.class.getName(), "IPv6 multicast send failed on " + iface.getName(), e);
            }
        }
    }

    private static void sendIpv6MulticastFallback(Device device) {
        try {
            DatagramPacket packet = PacketBuilder.buildMagicPacket(
                    IPV6_ALL_NODES, device.macAddress, device.port, device.secureOnPassword);
            for (NetworkInterface iface : getWolNetworkInterfaces()) {
                if (!hasIpv6Address(iface)) continue;
                try (MulticastSocket socket = new MulticastSocket()) {
                    socket.setNetworkInterface(iface);
                    socket.send(packet);
                } catch (IOException e) {
                    Log.w(WolSender.class.getName(), "IPv6 multicast fallback failed on " + iface.getName(), e);
                }
            }
        } catch (Exception e) {
            Log.w(WolSender.class.getName(), "IPv6 multicast fallback error", e);
        }
    }

    private static List<NetworkInterface> getWolNetworkInterfaces() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .filter(iface -> WOL_IFACE_PREFIXES.stream()
                            .anyMatch(prefix -> iface.getName().startsWith(prefix)))
                    .collect(Collectors.toList());
        } catch (SocketException e) {
            return Collections.emptyList();
        }
    }

    private static boolean hasIpv6Address(NetworkInterface iface) {
        return iface.getInterfaceAddresses().stream()
                .anyMatch(ia -> ia.getAddress() instanceof Inet6Address);
    }
}
