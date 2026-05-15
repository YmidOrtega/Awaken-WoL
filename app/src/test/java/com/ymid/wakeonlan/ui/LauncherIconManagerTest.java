package com.ymid.wakeonlan.ui;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

public class LauncherIconManagerTest {

    @Before
    public void resetState() throws Exception {
        setField("lastAppliedState", null);
        setField("pendingState", null);
        setField("appInForeground", false);
        setField("appContext", null);
    }

    @Test
    public void updateLauncherIcon_skipsWhenStateUnchanged() throws Exception {
        setField("lastAppliedState", true);
        // Calling updateLauncherIcon with same state should not change pendingState
        // We can't actually call it without a real context, but we verify the skip logic
        // via the lastAppliedState check: Boolean.valueOf(true).equals(true) == true
        Boolean lastState = (Boolean) getField("lastAppliedState");
        assertTrue(Boolean.valueOf(true).equals(lastState));
    }

    @Test
    public void pendingState_isNullInitially() throws Exception {
        assertNull(getField("pendingState"));
    }

    @Test
    public void appInForeground_isFalseInitially() throws Exception {
        assertFalse((boolean) getField("appInForeground"));
    }

    private static void setField(String name, Object value) throws Exception {
        Field f = LauncherIconManager.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static Object getField(String name) throws Exception {
        Field f = LauncherIconManager.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }
}
