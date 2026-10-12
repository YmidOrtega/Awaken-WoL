package com.ymid.wakeonlan.appactions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Assistant puts in wakeonlan://action/feature?name=… for OPEN_APP_FEATURE.
 * For an inline-inventory match it sends the shortcut id, not the spoken words: "shutdown_3" from
 * a device shortcut (see DeviceShortcutMapper) or "SCAN" from res/xml/shortcuts.xml. Anything else
 * is treated as a phrase and parsed like a command bar entry.
 */
public final class AssistantFeature {

    public enum Kind { WAKE_DEVICE, SHUTDOWN_DEVICE, OPEN_SCAN, PHRASE }

    public static final String SCAN_SHORTCUT_ID = "SCAN";
    private static final Pattern DEVICE_SHORTCUT_ID = Pattern.compile("(wake|shutdown)_(\\d+)");

    @NonNull
    public final Kind kind;
    /** Device id for WAKE_DEVICE and SHUTDOWN_DEVICE, otherwise -1. */
    public final int deviceId;
    /** The raw value, used as the phrase for PHRASE. */
    @NonNull
    public final String value;

    private AssistantFeature(@NonNull Kind kind, int deviceId, @NonNull String value) {
        this.kind = kind;
        this.deviceId = deviceId;
        this.value = value;
    }

    @NonNull
    public static AssistantFeature parse(@Nullable String value) {
        String text = value == null ? "" : value.trim();
        if (SCAN_SHORTCUT_ID.equals(text)) {
            return new AssistantFeature(Kind.OPEN_SCAN, -1, text);
        }
        Matcher matcher = DEVICE_SHORTCUT_ID.matcher(text);
        if (matcher.matches()) {
            try {
                int id = Integer.parseInt(matcher.group(2));
                Kind kind = "wake".equals(matcher.group(1)) ? Kind.WAKE_DEVICE : Kind.SHUTDOWN_DEVICE;
                return new AssistantFeature(kind, id, text);
            } catch (NumberFormatException ignored) {
                // An id too large for int is not one of ours; fall through to phrase parsing
            }
        }
        return new AssistantFeature(Kind.PHRASE, -1, text);
    }
}
