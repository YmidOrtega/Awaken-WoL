package com.ymid.wakeonlan.shutdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ymid.wakeonlan.persistence.models.Device;

import org.junit.Test;

import java.util.Optional;

public class ShutdownModelFactoryTest {

    // ── fromDevice: missing required fields → empty ───────────────────────────

    @Test
    public void fromDevice_shutdownDisabled_returnsEmpty() {
        Device d = passwordDevice();
        d.remoteShutdownEnabled = false;
        assertFalse(ShutdownModelFactory.fromDevice(d).isPresent());
    }

    @Test
    public void fromDevice_missingAddress_returnsEmpty() {
        Device d = passwordDevice();
        d.sshAddress = null;
        d.statusIp = null;
        assertFalse(ShutdownModelFactory.fromDevice(d).isPresent());
    }

    @Test
    public void fromDevice_missingUsername_returnsEmpty() {
        Device d = passwordDevice();
        d.sshUsername = null;
        assertFalse(ShutdownModelFactory.fromDevice(d).isPresent());
    }

    @Test
    public void fromDevice_missingPassword_passwordAuth_returnsEmpty() {
        Device d = passwordDevice();
        d.sshPassword = null;
        assertFalse(ShutdownModelFactory.fromDevice(d).isPresent());
    }

    @Test
    public void fromDevice_missingCommand_returnsEmpty() {
        Device d = passwordDevice();
        d.sshCommand = null;
        assertFalse(ShutdownModelFactory.fromDevice(d).isPresent());
    }

    // ── fromDevice: valid password auth ──────────────────────────────────────

    @Test
    public void fromDevice_validPasswordAuth_returnsModel() {
        Optional<ShutdownModel> result = ShutdownModelFactory.fromDevice(passwordDevice());
        assertTrue(result.isPresent());
        ShutdownModel m = result.get();
        assertEquals("192.168.1.100", m.getSshAddress());
        assertEquals(22, m.getSshPort());
        assertEquals("user", m.getUsername());
        assertEquals("pass", m.getPassword());
        assertEquals("shutdown -h now", m.getCommand());
        assertFalse(m.isKeyAuth());
    }

    @Test
    public void fromDevice_addressFallsBackToStatusIp() {
        Device d = passwordDevice();
        d.sshAddress = null;
        d.statusIp = "10.0.0.1";
        Optional<ShutdownModel> result = ShutdownModelFactory.fromDevice(d);
        assertTrue(result.isPresent());
        assertEquals("10.0.0.1", result.get().getSshAddress());
    }

    @Test
    public void fromDevice_customPort_usedInModel() {
        Device d = passwordDevice();
        d.sshPort = 2222;
        assertEquals(2222, ShutdownModelFactory.fromDevice(d).get().getSshPort());
    }

    @Test
    public void fromDevice_nullPort_defaultsTo22() {
        Device d = passwordDevice();
        d.sshPort = null;
        assertEquals(22, ShutdownModelFactory.fromDevice(d).get().getSshPort());
    }

    @Test
    public void fromDevice_zeroPort_defaultsTo22() {
        Device d = passwordDevice();
        d.sshPort = 0;
        assertEquals(22, ShutdownModelFactory.fromDevice(d).get().getSshPort());
    }

    // ── fromDevice: key auth ─────────────────────────────────────────────────

    @Test
    public void fromDevice_validKeyAuth_returnsModel() {
        Optional<ShutdownModel> result = ShutdownModelFactory.fromDevice(keyAuthDevice());
        assertTrue(result.isPresent());
        assertTrue(result.get().isKeyAuth());
        assertEquals("aweken_ssh_device1", result.get().getSshKeyAlias());
    }

    @Test
    public void fromDevice_keyAuth_missingAlias_returnsEmpty() {
        Device d = keyAuthDevice();
        d.sshKeyAlias = null;
        assertFalse(ShutdownModelFactory.fromDevice(d).isPresent());
    }

    @Test
    public void fromDevice_keyAuth_emptyAlias_returnsEmpty() {
        Device d = keyAuthDevice();
        d.sshKeyAlias = "";
        assertFalse(ShutdownModelFactory.fromDevice(d).isPresent());
    }

    @Test
    public void fromDevice_keyAuth_noPasswordRequired() {
        Device d = keyAuthDevice();
        d.sshPassword = null;
        assertTrue(ShutdownModelFactory.fromDevice(d).isPresent());
    }

    // ── ShutdownModel.isKeyAuth ───────────────────────────────────────────────

    @Test
    public void isKeyAuth_caseInsensitive() {
        ShutdownModel m = new ShutdownModel("h", 22, "u", null, "cmd", "KEY", "alias");
        assertTrue(m.isKeyAuth());
    }

    @Test
    public void isKeyAuth_nullAuthType_defaultsToPassword() {
        ShutdownModel m = new ShutdownModel("h", 22, "u", "p", "cmd", null, null);
        assertFalse(m.isKeyAuth());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Device passwordDevice() {
        Device d = new Device();
        d.remoteShutdownEnabled = true;
        d.sshAddress = "192.168.1.100";
        d.sshPort = 22;
        d.sshUsername = "user";
        d.sshPassword = "pass";
        d.sshCommand = "shutdown -h now";
        d.sshAuthType = "password";
        return d;
    }

    private static Device keyAuthDevice() {
        Device d = new Device();
        d.remoteShutdownEnabled = true;
        d.sshAddress = "192.168.1.100";
        d.sshPort = 22;
        d.sshUsername = "user";
        d.sshCommand = "shutdown -h now";
        d.sshAuthType = "key";
        d.sshKeyAlias = "aweken_ssh_device1";
        return d;
    }
}
