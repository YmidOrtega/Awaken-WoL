package com.ymid.wakeonlan.ui.modify.watcher.validator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SecureOnPasswordValidatorTest {

    private final SecureOnPasswordValidator validator = new SecureOnPasswordValidator(null);

    // Empty is always valid (field is optional)
    @Test public void empty_valid()             { assertValid(""); }
    @Test public void null_valid()              { assertValid(null); }

    // Valid MAC address as SecureOn password
    @Test public void macColons_valid()         { assertValid("AA:BB:CC:DD:EE:FF"); }
    @Test public void macDashes_valid()         { assertValid("AA-BB-CC-DD-EE-FF"); }
    @Test public void macLowercase_valid()      { assertValid("aa:bb:cc:dd:ee:ff"); }

    // Valid IPv4 address as SecureOn password
    @Test public void ipv4_valid()              { assertValid("192.168.1.1"); }
    @Test public void ipv4Broadcast_valid()     { assertValid("255.255.255.255"); }

    // Arbitrary string is invalid
    @Test public void arbitraryString_invalid() { assertInvalid("mysecret"); }
    @Test public void partialMac_invalid()      { assertInvalid("AA:BB:CC"); }
    @Test public void mixedSepMac_invalid()     { assertInvalid("AA:BB-CC:DD:EE:FF"); }

    private void assertValid(String input) {
        assertEquals(ValidationResult.VALID, validator.validate(input));
    }

    private void assertInvalid(String input) {
        assertEquals(ValidationResult.INVALID, validator.validate(input));
    }
}
