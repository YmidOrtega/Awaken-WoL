package com.ymid.wakeonlan.ui.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.ymid.wakeonlan.persistence.models.Device;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ConsoleCommandTest {

    private final Device ubuntu = device(1, "Ubuntu");
    private final Device gamingPc = device(2, "Gaming PC");
    private final Device gamingLaptop = device(3, "Gaming Laptop");
    private final List<Device> devices = Arrays.asList(ubuntu, gamingPc, gamingLaptop);

    // ── verbs ────────────────────────────────────────────────────────────────

    @Test
    public void parse_spanishWakeVerb_returnsWake() {
        ConsoleCommand command = ConsoleCommand.parse("encender ubuntu", devices);
        assertEquals(ConsoleCommand.Action.WAKE, command.action);
        assertSame(ubuntu, command.device);
        assertEquals(ConsoleCommand.Error.NONE, command.error);
    }

    @Test
    public void parse_englishAndGermanVerbs_areAccepted() {
        assertEquals(ConsoleCommand.Action.WAKE, ConsoleCommand.parse("wake ubuntu", devices).action);
        assertEquals(ConsoleCommand.Action.SHUTDOWN, ConsoleCommand.parse("aus ubuntu", devices).action);
        assertEquals(ConsoleCommand.Action.EDIT, ConsoleCommand.parse("bearbeiten ubuntu", devices).action);
    }

    @Test
    public void parse_verbIsCaseInsensitive() {
        assertEquals(ConsoleCommand.Action.SHUTDOWN, ConsoleCommand.parse("APAGAR Ubuntu", devices).action);
    }

    @Test
    public void parse_addNeedsNoDevice() {
        ConsoleCommand command = ConsoleCommand.parse("agregar", devices);
        assertEquals(ConsoleCommand.Action.ADD, command.action);
        assertEquals(ConsoleCommand.Error.NONE, command.error);
    }

    @Test
    public void parse_unknownVerb_reportsError() {
        assertEquals(ConsoleCommand.Error.UNKNOWN_VERB, ConsoleCommand.parse("reboot ubuntu", devices).error);
    }

    @Test
    public void parse_blankInput_reportsEmpty() {
        assertEquals(ConsoleCommand.Error.EMPTY, ConsoleCommand.parse("   ", devices).error);
        assertEquals(ConsoleCommand.Error.EMPTY, ConsoleCommand.parse(null, devices).error);
    }

    @Test
    public void parse_verbWithoutDevice_reportsMissingDevice() {
        assertEquals(ConsoleCommand.Error.MISSING_DEVICE, ConsoleCommand.parse("encender", devices).error);
    }

    // ── spoken commands ──────────────────────────────────────────────────────

    @Test
    public void parse_spanishImperatives_areAccepted() {
        assertEquals(ConsoleCommand.Action.SHUTDOWN, ConsoleCommand.parse("Apaga Ubuntu", devices).action);
        assertEquals(ConsoleCommand.Action.WAKE, ConsoleCommand.parse("Enciende Ubuntu", devices).action);
        assertEquals(ConsoleCommand.Action.WAKE, ConsoleCommand.parse("prende ubuntu", devices).action);
    }

    @Test
    public void parse_englishPhrasalVerbs_areFolded() {
        ConsoleCommand command = ConsoleCommand.parse("turn off gaming pc", devices);
        assertEquals(ConsoleCommand.Action.SHUTDOWN, command.action);
        assertSame(gamingPc, command.device);
        assertEquals(ConsoleCommand.Action.WAKE, ConsoleCommand.parse("Turn On Ubuntu", devices).action);
        assertEquals(ConsoleCommand.Action.SHUTDOWN, ConsoleCommand.parse("shut down ubuntu", devices).action);
    }

    @Test
    public void parse_leadingFillers_areIgnored() {
        assertSame(ubuntu, ConsoleCommand.parse("apaga mi Ubuntu", devices).device);
        assertSame(gamingPc, ConsoleCommand.parse("turn on the gaming pc", devices).device);
    }

    @Test
    public void parse_fillerAlone_isKeptAsDeviceName() {
        Device myDevice = device(4, "Mi");
        assertSame(myDevice, ConsoleCommand.parse("apaga mi", Arrays.asList(ubuntu, myDevice)).device);
    }

    // ── device matching ──────────────────────────────────────────────────────

    @Test
    public void parse_matchesSlugOrSpacedName() {
        assertSame(gamingPc, ConsoleCommand.parse("encender gaming-pc", devices).device);
        assertSame(gamingPc, ConsoleCommand.parse("encender Gaming  PC", devices).device);
    }

    @Test
    public void parse_uniquePrefix_matchesDevice() {
        assertSame(ubuntu, ConsoleCommand.parse("encender ubu", devices).device);
    }

    @Test
    public void parse_ambiguousPrefix_reportsNotFound() {
        ConsoleCommand command = ConsoleCommand.parse("encender gaming", devices);
        assertNull(command.device);
        assertEquals(ConsoleCommand.Error.DEVICE_NOT_FOUND, command.error);
        assertEquals("gaming", command.query);
    }

    @Test
    public void slug_collapsesWhitespaceAndLowercases() {
        assertEquals("gaming-pc", ConsoleCommand.slug("  Gaming   PC "));
        assertEquals("", ConsoleCommand.slug(null));
    }

    private static Device device(int id, String name) {
        Device device = new Device();
        device.id = id;
        device.name = name;
        return device;
    }
}
