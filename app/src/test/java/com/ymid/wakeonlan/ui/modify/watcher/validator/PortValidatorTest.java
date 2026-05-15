package com.ymid.wakeonlan.ui.modify.watcher.validator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PortValidatorTest {

    private final PortValidator validator = new PortValidator(null);

    @Test public void port_typical()        { assertValid("9"); }
    @Test public void port_min()            { assertValid("1"); }
    @Test public void port_max()            { assertValid("65535"); }
    @Test public void port_commonHttp()     { assertValid("80"); }
    @Test public void port_empty()          { assertValid(""); }
    @Test public void port_null()           { assertValid(null); }

    @Test public void port_zero()           { assertInvalid("0"); }
    @Test public void port_negative()       { assertInvalid("-1"); }
    @Test public void port_tooLarge()       { assertInvalid("65536"); }
    @Test public void port_letters()        { assertInvalid("abc"); }
    @Test public void port_alphanumeric()   { assertInvalid("80a"); }
    @Test public void port_decimal()        { assertInvalid("9.5"); }

    private void assertValid(String input) {
        assertEquals(ValidationResult.VALID, validator.validate(input));
    }

    private void assertInvalid(String input) {
        assertEquals(ValidationResult.INVALID, validator.validate(input));
    }
}
