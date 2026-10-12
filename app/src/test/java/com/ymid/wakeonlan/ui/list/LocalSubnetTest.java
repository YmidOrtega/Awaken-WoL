package com.ymid.wakeonlan.ui.list;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LocalSubnetTest {

    @Test
    public void toCidr_masksHostBits() {
        assertEquals("192.168.1.0/24", LocalSubnet.toCidr(new byte[]{(byte) 192, (byte) 168, 1, 40}, 24));
    }

    @Test
    public void toCidr_handlesNonOctetPrefix() {
        assertEquals("10.0.16.0/20", LocalSubnet.toCidr(new byte[]{10, 0, 23, (byte) 200}, 20));
    }

    @Test
    public void toCidr_zeroPrefix_coversEverything() {
        assertEquals("0.0.0.0/0", LocalSubnet.toCidr(new byte[]{(byte) 172, 16, 5, 9}, 0));
    }
}
