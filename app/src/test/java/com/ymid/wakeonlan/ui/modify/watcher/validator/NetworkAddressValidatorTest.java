package com.ymid.wakeonlan.ui.modify.watcher.validator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NetworkAddressValidatorTest {

    private final NetworkAddressValidator required = new NetworkAddressValidator(null, false);
    private final NetworkAddressValidator optional = new NetworkAddressValidator(null, true);

    // ── IPv4 ──────────────────────────────────────────────────────────────────

    @Test public void ipv4_valid()              { assertValid(required, "192.168.1.255"); }
    @Test public void ipv4_loopback()           { assertValid(required, "127.0.0.1"); }
    @Test public void ipv4_broadcast()          { assertValid(required, "255.255.255.255"); }
    @Test public void ipv4_zeros()              { assertValid(required, "0.0.0.0"); }

    // These look like bad IPs but are accepted as valid hostnames by design
    @Test public void ipv4_tooFewOctets_validHostname()   { assertValid(required, "192.168.1"); }
    @Test public void ipv4_tooManyOctets_validHostname()  { assertValid(required, "192.168.1.1.1"); }
    @Test public void ipv4_octetOver255_validHostname()   { assertValid(required, "192.168.1.256"); }

    // Leading hyphen in a label: not valid hostname, not valid IP → INVALID
    @Test public void ipv4_octetNegative_invalid() { assertInvalid(required, "192.168.-1.1"); }

    // ── IPv6 ──────────────────────────────────────────────────────────────────

    @Test public void ipv6_full()               { assertValid(required, "2001:0db8:85a3:0000:0000:8a2e:0370:7334"); }
    @Test public void ipv6_compressed()         { assertValid(required, "2001:db8::1"); }
    @Test public void ipv6_loopback()           { assertValid(required, "::1"); }
    @Test public void ipv6_linkLocal()          { assertValid(required, "fe80::1"); }
    @Test public void ipv6_bracketed()          { assertValid(required, "[2001:db8::1]"); }
    @Test public void ipv6_invalid()            { assertInvalid(required, "[2001:db8::gggg]"); }

    // ── Hostname ──────────────────────────────────────────────────────────────

    @Test public void hostname_simple()             { assertValid(required, "myserver"); }
    @Test public void hostname_fqdn()               { assertValid(required, "server.example.com"); }
    @Test public void hostname_withNumbers()         { assertValid(required, "server01.local"); }
    @Test public void hostname_hyphenInMiddle()      { assertValid(required, "my-server.example.com"); }
    @Test public void hostname_lettersLookingLikeIp(){ assertValid(required, "abc.def.ghi.jkl"); }

    @Test public void hostname_startsWithHyphen()    { assertInvalid(required, "-server.example.com"); }
    @Test public void hostname_endsWithHyphen()      { assertInvalid(required, "server-.example.com"); }
    @Test public void hostname_underscore()          { assertInvalid(required, "my_server.local"); }
    @Test public void hostname_atSign()              { assertInvalid(required, "user@host"); }

    // ── Empty / null ──────────────────────────────────────────────────────────

    @Test public void empty_required_invalid()   { assertInvalid(required, ""); }
    @Test public void empty_optional_valid()     { assertValid(optional, ""); }
    @Test public void null_optional_valid()      { assertValid(optional, null); }
    @Test public void whitespaceOnly_optional()  { assertValid(optional, "   "); }

    private static void assertValid(NetworkAddressValidator v, String input) {
        assertEquals(ValidationResult.VALID, v.validate(input));
    }

    private static void assertInvalid(NetworkAddressValidator v, String input) {
        assertEquals(ValidationResult.INVALID, v.validate(input));
    }
}
