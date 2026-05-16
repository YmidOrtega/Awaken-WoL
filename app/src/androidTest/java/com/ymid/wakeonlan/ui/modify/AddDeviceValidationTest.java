package com.ymid.wakeonlan.ui.modify;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ymid.wakeonlan.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for the Add Device form validation.
 * Run on a device/emulator: ./gradlew :app:connectedAndroidTest
 */
@RunWith(AndroidJUnit4.class)
public class AddDeviceValidationTest {

    @Rule
    public ActivityScenarioRule<AddDeviceActivity> activityRule =
            new ActivityScenarioRule<>(AddDeviceActivity.class);

    // ── Name validation ───────────────────────────────────────────────────────

    @Test
    public void save_withEmptyName_showsNameError() {
        onView(withId(R.id.add_device_menu_save)).perform(click());
        onView(withId(R.id.device_name))
                .check(matches(hasErrorText("Name must not be empty")));
    }

    @Test
    public void save_withValidName_noNameError() {
        onView(withId(R.id.device_name)).perform(typeText("My PC"), closeSoftKeyboard());
        onView(withId(R.id.add_device_menu_save)).perform(click());
        // Name field is valid — error must be absent
        onView(withId(R.id.device_name)).check(matches(hasErrorText(null)));
    }

    // ── MAC validation ────────────────────────────────────────────────────────

    @Test
    public void save_withEmptyMac_showsMacError() {
        onView(withId(R.id.device_name)).perform(typeText("My PC"), closeSoftKeyboard());
        onView(withId(R.id.add_device_menu_save)).perform(click());
        onView(withId(R.id.device_mac))
                .check(matches(hasErrorText("Invalid MAC Address")));
    }

    @Test
    public void save_withInvalidMac_showsMacError() {
        onView(withId(R.id.device_name)).perform(typeText("My PC"), closeSoftKeyboard());
        onView(withId(R.id.device_mac))
                .perform(click(), replaceText("not-a-mac"), closeSoftKeyboard());
        onView(withId(R.id.add_device_menu_save)).perform(click());
        onView(withId(R.id.device_mac))
                .check(matches(hasErrorText("Invalid MAC Address")));
    }

    @Test
    public void mac_withValidColonSeparator_noMacError() {
        onView(withId(R.id.device_mac))
                .perform(click(), replaceText("AB:12:CD:34:EF:56"), closeSoftKeyboard());
        onView(withId(R.id.device_mac)).check(matches(hasErrorText(null)));
    }

    @Test
    public void mac_withValidDashSeparator_noMacError() {
        onView(withId(R.id.device_mac))
                .perform(click(), replaceText("AB-12-CD-34-EF-56"), closeSoftKeyboard());
        onView(withId(R.id.device_mac)).check(matches(hasErrorText(null)));
    }

    @Test
    public void mac_withMixedSeparators_showsMacError() {
        onView(withId(R.id.device_mac))
                .perform(click(), replaceText("AB:12-CD:34:EF:56"), closeSoftKeyboard());
        onView(withId(R.id.device_mac))
                .check(matches(hasErrorText("Invalid MAC Address")));
    }

    // ── Form layout sanity ────────────────────────────────────────────────────

    @Test
    public void formFields_areVisible() {
        onView(withId(R.id.device_name)).check(matches(isDisplayed()));
        onView(withId(R.id.device_mac)).check(matches(isDisplayed()));
    }
}
