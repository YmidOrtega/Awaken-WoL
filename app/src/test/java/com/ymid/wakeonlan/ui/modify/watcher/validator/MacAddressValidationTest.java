package com.ymid.wakeonlan.ui.modify.watcher.validator;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.regex.Pattern;

public class MacAddressValidationTest {

    private static final Pattern MAC_PATTERN =
            Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    private boolean isValidMac(String mac) {
        return MAC_PATTERN.matcher(mac.trim()).matches();
    }

    @Test public void validMacWithColons() { assertTrue(isValidMac("AB:12:CD:34:EF:56")); }
    @Test public void validMacWithDashes()  { assertTrue(isValidMac("AB-12-CD-34-EF-56")); }
    @Test public void validMacLowercase()   { assertTrue(isValidMac("ab:12:cd:34:ef:56")); }
    @Test public void validMacAllZeros()    { assertTrue(isValidMac("00:00:00:00:00:00")); }
    @Test public void validMacBroadcast()   { assertTrue(isValidMac("FF:FF:FF:FF:FF:FF")); }

    @Test public void invalidMacTooShort()  { assertFalse(isValidMac("AB:12:CD:34:EF")); }
    @Test public void invalidMacTooLong()   { assertFalse(isValidMac("AB:12:CD:34:EF:56:78")); }
    @Test public void invalidMacNoSep()     { assertFalse(isValidMac("AB12CD34EF56")); }
    @Test public void invalidMacBadChar()   { assertFalse(isValidMac("ZZ:12:CD:34:EF:56")); }
    @Test public void invalidMacEmpty()     { assertFalse(isValidMac("")); }
    @Test public void invalidMacSpaces()    { assertFalse(isValidMac("AB 12 CD 34 EF 56")); }
    @Test public void invalidMacMixed()     { assertFalse(isValidMac("AB:12-CD:34:EF:56")); }
}
