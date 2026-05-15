package com.ymid.wakeonlan.ui.modify.watcher.validator;

import android.widget.EditText;

import com.google.common.base.Strings;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.regex.Pattern;

import com.ymid.wakeonlan.R;

public class NetworkAddressValidator extends Validator {

    private static final Pattern HOSTNAME_PATTERN =
            Pattern.compile("^(?=.{1,253}$)([A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)*[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$");

    private final boolean inputIsOptional;

    public NetworkAddressValidator(EditText editTextView, boolean inputIsOptional) {
        super(editTextView);
        this.inputIsOptional = inputIsOptional;
    }

    @Override
    public ValidationResult validate(String text) {
        String value = Strings.nullToEmpty(text).trim();
        if (value.isEmpty()) {
            return inputIsOptional ? ValidationResult.VALID : ValidationResult.INVALID;
        }

        return isValidIpv4(value) || isValidIpv6(value) || isValidHostname(value)
                ? ValidationResult.VALID
                : ValidationResult.INVALID;
    }

    @Override
    int getErrorMessageStringId() {
        return R.string.add_device_error_address_invalid;
    }

    private boolean isValidIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return false;

        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) return false;
            try {
                int octet = Integer.parseInt(part);
                if (octet < 0 || octet > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidIpv6(String value) {
        String normalized = normalizeBracketedIpv6(value);
        if (!normalized.contains(":")) return false;

        try {
            return InetAddress.getByName(normalized) instanceof Inet6Address;
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeBracketedIpv6(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private boolean isValidHostname(String value) {
        if (value.contains(":") || value.contains("_") || value.startsWith("-") || value.endsWith("-")) {
            return false;
        }
        return HOSTNAME_PATTERN.matcher(value).matches();
    }
}
