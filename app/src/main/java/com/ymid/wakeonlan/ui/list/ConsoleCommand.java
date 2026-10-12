package com.ymid.wakeonlan.ui.list;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ymid.wakeonlan.persistence.models.Device;

/**
 * Parses the device list command bar, e.g. "encender ubuntu" or "off gaming-pc".
 * Verbs are accepted in every app language so muscle memory survives a language switch.
 */
public final class ConsoleCommand {

    public enum Action { WAKE, SHUTDOWN, EDIT, ADD }

    public enum Error { NONE, EMPTY, UNKNOWN_VERB, MISSING_DEVICE, DEVICE_NOT_FOUND }

    private static final Set<String> WAKE_VERBS = verbs("wake", "on", "encender", "prender", "despertar", "wecken", "einschalten");
    private static final Set<String> SHUTDOWN_VERBS = verbs("off", "shutdown", "apagar", "aus", "herunterfahren");
    private static final Set<String> EDIT_VERBS = verbs("edit", "editar", "bearbeiten");
    private static final Set<String> ADD_VERBS = verbs("add", "new", "agregar", "añadir", "nuevo", "hinzufügen");

    @Nullable
    public final Action action;
    @Nullable
    public final Device device;
    @NonNull
    public final Error error;
    /** The device part of the input, kept for error messages. */
    @NonNull
    public final String query;

    private ConsoleCommand(@Nullable Action action, @Nullable Device device, @NonNull Error error, @NonNull String query) {
        this.action = action;
        this.device = device;
        this.error = error;
        this.query = query;
    }

    @NonNull
    public static ConsoleCommand parse(@Nullable String input, @NonNull List<Device> devices) {
        String text = input == null ? "" : input.trim();
        if (text.isEmpty()) {
            return new ConsoleCommand(null, null, Error.EMPTY, "");
        }

        String[] parts = text.split("\\s+", 2);
        Action action = actionFor(parts[0].toLowerCase(Locale.ROOT));
        String query = parts.length > 1 ? parts[1].trim() : "";
        if (action == null) {
            return new ConsoleCommand(null, null, Error.UNKNOWN_VERB, query);
        }
        if (action == Action.ADD) {
            return new ConsoleCommand(action, null, Error.NONE, query);
        }
        if (query.isEmpty()) {
            return new ConsoleCommand(action, null, Error.MISSING_DEVICE, query);
        }

        Device device = findDevice(query, devices);
        return device == null
                ? new ConsoleCommand(action, null, Error.DEVICE_NOT_FOUND, query)
                : new ConsoleCommand(action, device, Error.NONE, query);
    }

    /** Lower-case, dash-separated form of a device name: "Gaming PC" becomes "gaming-pc". */
    @NonNull
    public static String slug(@Nullable String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }

    @Nullable
    private static Device findDevice(String query, List<Device> devices) {
        String wanted = slug(query);
        for (Device device : devices) {
            if (slug(device.name).equals(wanted)) {
                return device;
            }
        }
        // Fall back to a prefix, but only when it points at exactly one device
        List<Device> prefixMatches = new ArrayList<>();
        for (Device device : devices) {
            if (slug(device.name).startsWith(wanted)) {
                prefixMatches.add(device);
            }
        }
        return prefixMatches.size() == 1 ? prefixMatches.get(0) : null;
    }

    @Nullable
    private static Action actionFor(String verb) {
        if (WAKE_VERBS.contains(verb)) return Action.WAKE;
        if (SHUTDOWN_VERBS.contains(verb)) return Action.SHUTDOWN;
        if (EDIT_VERBS.contains(verb)) return Action.EDIT;
        if (ADD_VERBS.contains(verb)) return Action.ADD;
        return null;
    }

    private static Set<String> verbs(String... verbs) {
        return new HashSet<>(Arrays.asList(verbs));
    }
}
