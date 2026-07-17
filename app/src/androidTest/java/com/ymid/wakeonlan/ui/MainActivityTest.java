package com.ymid.wakeonlan.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.ui.modify.AddDeviceActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ctx.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("done", true)
                .commit();

        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ── Layout sanity ─────────────────────────────────────────────────────────

    @Test
    public void launches_showsFab() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.add_device_fab)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void launches_showsDeviceList() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.machine_list)).check(matches(isDisplayed()));
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Test
    public void fabClick_opensAddDeviceActivity() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.add_device_fab)).perform(click());
            intended(hasComponent(AddDeviceActivity.class.getName()));
        }
    }
}
