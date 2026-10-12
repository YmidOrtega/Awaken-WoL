package com.ymid.wakeonlan.appactions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AssistantFeatureTest {

    @Test
    public void parse_deviceShortcutIds_resolveToDeviceActions() {
        AssistantFeature shutdown = AssistantFeature.parse("shutdown_3");
        assertEquals(AssistantFeature.Kind.SHUTDOWN_DEVICE, shutdown.kind);
        assertEquals(3, shutdown.deviceId);

        AssistantFeature wake = AssistantFeature.parse("wake_12");
        assertEquals(AssistantFeature.Kind.WAKE_DEVICE, wake.kind);
        assertEquals(12, wake.deviceId);
    }

    @Test
    public void parse_scanShortcutId_opensScan() {
        assertEquals(AssistantFeature.Kind.OPEN_SCAN, AssistantFeature.parse("SCAN").kind);
    }

    @Test
    public void parse_anythingElse_isAPhrase() {
        AssistantFeature phrase = AssistantFeature.parse("  Apaga Ubuntu ");
        assertEquals(AssistantFeature.Kind.PHRASE, phrase.kind);
        assertEquals("Apaga Ubuntu", phrase.value);
        assertEquals(-1, phrase.deviceId);
    }

    @Test
    public void parse_lookalikeIds_stayPhrases() {
        assertEquals(AssistantFeature.Kind.PHRASE, AssistantFeature.parse("shutdown_").kind);
        assertEquals(AssistantFeature.Kind.PHRASE, AssistantFeature.parse("reboot_3").kind);
        assertEquals(AssistantFeature.Kind.PHRASE, AssistantFeature.parse("shutdown_99999999999").kind);
        assertEquals(AssistantFeature.Kind.PHRASE, AssistantFeature.parse(null).kind);
    }
}
