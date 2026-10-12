package com.ymid.wakeonlan.ui.list;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;

import androidx.annotation.Nullable;

import java.net.Inet4Address;
import java.net.InetAddress;

/** Describes the active local IPv4 network as CIDR, e.g. "192.168.1.0/24". */
public final class LocalSubnet {

    private LocalSubnet() {
    }

    @Nullable
    public static String describe(Context context) {
        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
        if (connectivityManager == null) {
            return null;
        }
        Network network = connectivityManager.getActiveNetwork();
        LinkProperties properties = network == null ? null : connectivityManager.getLinkProperties(network);
        if (properties == null) {
            return null;
        }
        for (LinkAddress linkAddress : properties.getLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                return toCidr(address.getAddress(), linkAddress.getPrefixLength());
            }
        }
        return null;
    }

    static String toCidr(byte[] address, int prefixLength) {
        int value = ((address[0] & 0xFF) << 24) | ((address[1] & 0xFF) << 16) | ((address[2] & 0xFF) << 8) | (address[3] & 0xFF);
        int mask = prefixLength == 0 ? 0 : -1 << (32 - prefixLength);
        int network = value & mask;
        return ((network >>> 24) & 0xFF) + "." + ((network >>> 16) & 0xFF) + "." + ((network >>> 8) & 0xFF) + "." + (network & 0xFF)
                + "/" + prefixLength;
    }
}
