package com.ymid.wakeonlan.shutdown;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Tests the private isDangerousCommand logic via reflection.
 * This is the safety gate used in test/validation mode to block destructive commands.
 */
public class ShutdownDangerousCommandTest {

    private Method isDangerous;
    private ShutdownRunnable runnable;

    @Before
    public void setUp() throws Exception {
        ShutdownModel model = new ShutdownModel("host", 22, "user", "pass", "cmd");
        runnable = new ShutdownRunnable(model, new com.ymid.wakeonlan.shutdown.listener.IgnoringShutdownExecutorListener());
        isDangerous = ShutdownRunnable.class.getDeclaredMethod("isDangerousCommand", String.class, String.class);
        isDangerous.setAccessible(true);
    }

    private boolean check(String command, String os) throws Exception {
        return (boolean) isDangerous.invoke(runnable, command, os);
    }

    // ── Linux ─────────────────────────────────────────────────────────────────

    @Test public void linux_poweroff()              throws Exception { assertTrue(check("poweroff", "linux")); }
    @Test public void linux_halt()                  throws Exception { assertTrue(check("halt", "linux")); }
    @Test public void linux_init0()                 throws Exception { assertTrue(check("init 0", "linux")); }
    @Test public void linux_shutdownNow()           throws Exception { assertTrue(check("shutdown -h now", "linux")); }
    @Test public void linux_shutdownDashP()         throws Exception { assertTrue(check("shutdown -P now", "linux")); }
    @Test public void linux_systemctlPoweroff()     throws Exception { assertTrue(check("systemctl poweroff", "linux")); }
    @Test public void linux_systemctlHalt()         throws Exception { assertTrue(check("systemctl halt", "linux")); }
    @Test public void linux_echo_safe()             throws Exception { assertFalse(check("echo hello", "linux")); }
    @Test public void linux_reboot_safe()           throws Exception { assertFalse(check("reboot", "linux")); }

    // ── Windows ──────────────────────────────────────────────────────────────

    @Test public void windows_shutdownSlashS()      throws Exception { assertTrue(check("shutdown /s /t 0", "windows")); }
    @Test public void windows_shutdownSlashP()      throws Exception { assertTrue(check("shutdown /p", "windows")); }
    @Test public void windows_shutdownExe()         throws Exception { assertTrue(check("shutdown.exe /s /t 0", "windows")); }
    @Test public void windows_echo_safe()           throws Exception { assertFalse(check("echo hello", "windows")); }

    // ── macOS ─────────────────────────────────────────────────────────────────

    @Test public void macos_shutdownNow()           throws Exception { assertTrue(check("shutdown -h now", "macos")); }
    @Test public void macos_osascript()             throws Exception { assertTrue(check("osascript -e 'tell app \"Finder\" to shut down'", "macos")); }
    @Test public void macos_echo_safe()             throws Exception { assertFalse(check("echo hello", "macos")); }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test public void null_command_safe()           throws Exception { assertFalse(check(null, "linux")); }
    @Test public void null_os_fallsBackToLinux()    throws Exception { assertTrue(check("poweroff", null)); }
}
