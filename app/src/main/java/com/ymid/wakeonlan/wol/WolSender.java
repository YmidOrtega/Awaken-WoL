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
            // Send to the device's configured broadcast/IP address
            sendPacket(device, device.broadcastAddress);

            // IPv4 auto-detected broadcast
            new BroadcastHelper().getBroadcastAddress()
                    .ifPresent(addr -> sendPacket(device, addr.getHostAddress()));

            // IPv6 multicast fallback: send to ff02::1 on every WiFi/eth interface
            // that has an IPv6 address, so WOL works on IPv6 networks without configuration.
            // Skipped if ff02::1 is already the configured address to avoid a duplicate send.
            if (!IPV6_ALL_NODES.equalsIgnoreCase(PacketBuilder.normalizeAddress(device.broadcastAddress))) {
                sendIpv6MulticastFallback(device);
            }

            // WAN / port-forwarded WOL
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

    // Used when the user explicitly configures ff02::1 or any IPv6 multicast address.
    // Sends the packet on every eligible network interface so it reaches the right LAN
    // segment regardless of which interface the OS would pick by default.
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

    // Automatic IPv6 equivalent of the BroadcastHelper IPv4 fallback.
    // Sends to ff02::1 (all-nodes link-local multicast) on each interface
    // that has at least one IPv6 address assigned.
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
