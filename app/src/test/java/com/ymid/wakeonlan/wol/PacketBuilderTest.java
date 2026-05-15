package com.ymid.wakeonlan.wol;

import static org.junit.Assert.*;

import org.junit.Test;

import java.net.DatagramPacket;
import java.net.Inet6Address;

public class PacketBuilderTest {

    private static final String MAC = "AB:12:CD:34:EF:56";
    private static final String BROADCAST = "192.168.1.255";
    private static final int PORT = 9;

    @Test
    public void buildMagicPacket_hasCorrectSize() throws Exception {
        DatagramPacket packet = PacketBuilder.buildMagicPacket(BROADCAST, MAC, PORT, null);
        // 6 bytes (0xff preamble) + 16 * 6 bytes (MAC repeated) = 102 bytes
        assertEquals(102, packet.getLength());
    }

    @Test
    public void buildMagicPacket_startsWithSixFfBytes() throws Exception {
        DatagramPacket packet = PacketBuilder.buildMagicPacket(BROADCAST, MAC, PORT, null);
        byte[] data = packet.getData();
        for (int i = 0; i < 6; i++) {
            assertEquals((byte) 0xFF, data[i]);
        }
    }

    @Test
    public void buildMagicPacket_containsMacRepeated16Times() throws Exception {
        DatagramPacket packet = PacketBuilder.buildMagicPacket(BROADCAST, MAC, PORT, null);
        byte[] data = packet.getData();
        byte[] expectedMac = {(byte) 0xAB, 0x12, (byte) 0xCD, 0x34, (byte) 0xEF, 0x56};
        for (int rep = 0; rep < 16; rep++) {
            for (int b = 0; b < 6; b++) {
                assertEquals("MAC mismatch at rep=" + rep + " byte=" + b,
                        expectedMac[b], data[6 + rep * 6 + b]);
            }
        }
    }

    @Test
    public void buildMagicPacket_withColonAndDashSeparator() throws Exception {
        DatagramPacket p1 = PacketBuilder.buildMagicPacket(BROADCAST, "AB:12:CD:34:EF:56", PORT, null);
        DatagramPacket p2 = PacketBuilder.buildMagicPacket(BROADCAST, "AB-12-CD-34-EF-56", PORT, null);
        assertArrayEquals(p1.getData(), p2.getData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildMagicPacket_invalidMacThrows() throws Exception {
        PacketBuilder.buildMagicPacket(BROADCAST, "not-a-mac", PORT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildMagicPacket_mixedMacSeparatorsThrows() throws Exception {
        PacketBuilder.buildMagicPacket(BROADCAST, "AB:12-CD:34:EF:56", PORT, null);
    }

    @Test
    public void isIpv6_detectsIpv6Address() {
        assertTrue(PacketBuilder.isIpv6("2001:db8::1"));
        assertTrue(PacketBuilder.isIpv6("fe80::1%wlan0"));
        assertTrue(PacketBuilder.isIpv6("[2001:db8::1]"));
        assertFalse(PacketBuilder.isIpv6("192.168.1.255"));
        assertFalse(PacketBuilder.isIpv6(null));
    }

    @Test
    public void normalizeAddress_keepsIpv6ScopeId() {
        assertEquals("fe80::1%wlan0", PacketBuilder.normalizeAddress("fe80::1%wlan0"));
    }

    @Test
    public void normalizeAddress_unwrapsBracketedIpv6() {
        assertEquals("2001:db8::1", PacketBuilder.normalizeAddress("[2001:db8::1]"));
    }

    @Test
    public void normalizeAddress_decodesEscapedIpv6ScopeId() {
        assertEquals("fe80::1%wlan0", PacketBuilder.normalizeAddress("fe80::1%25wlan0"));
    }

    @Test
    public void buildMagicPacket_targetsIpv6Address() throws Exception {
        DatagramPacket packet = PacketBuilder.buildMagicPacket("2001:db8::1", MAC, PORT, null);
        assertTrue(PacketBuilder.isIpv6(packet.getAddress().getHostAddress()));
    }

    @Test
    public void buildMagicPacket_targetsCorrectPort() throws Exception {
        DatagramPacket packet = PacketBuilder.buildMagicPacket(BROADCAST, MAC, 7, null);
        assertEquals(7, packet.getPort());
    }

    @Test
    public void buildMagicPacket_withSecureOnPassword_hasCorrectSize() throws Exception {
        // SecureOn as MAC address (6 bytes appended)
        DatagramPacket packet = PacketBuilder.buildMagicPacket(BROADCAST, MAC, PORT, "AA:BB:CC:DD:EE:FF");
        assertEquals(108, packet.getLength()); // 102 + 6
    }

    // ── IPv6 multicast (ff02::1) ──────────────────────────────────────────────

    @Test
    public void buildMagicPacket_ipv6AllNodes_resolvesToMulticastInet6Address() throws Exception {
        DatagramPacket packet = PacketBuilder.buildMagicPacket("ff02::1", MAC, PORT, null);
        assertTrue(packet.getAddress() instanceof Inet6Address);
        assertTrue(packet.getAddress().isMulticastAddress());
    }

    @Test
    public void buildMagicPacket_ipv6AllNodes_hasCorrectPayloadSize() throws Exception {
        DatagramPacket packet = PacketBuilder.buildMagicPacket("ff02::1", MAC, PORT, null);
        assertEquals(102, packet.getLength());
    }

    @Test
    public void buildMagicPacket_bracketedIpv6AllNodes_resolves() throws Exception {
        DatagramPacket packet = PacketBuilder.buildMagicPacket("[ff02::1]", MAC, PORT, null);
        assertTrue(packet.getAddress() instanceof Inet6Address);
        assertTrue(packet.getAddress().isMulticastAddress());
    }
}
