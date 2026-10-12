package com.ymid.wakeonlan.appactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import com.google.assistant.appactions.testing.aatl.AppActionsTestManager;
import com.google.assistant.appactions.testing.aatl.fulfillment.AppActionsFulfillmentIntentResult;
import com.google.assistant.appactions.testing.aatl.fulfillment.AppActionsFulfillmentResult;
import com.google.assistant.appactions.testing.aatl.fulfillment.FulfillmentType;
import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Locale;


/**
 * Checks that res/xml/shortcuts.xml turns an Assistant OPEN_APP_FEATURE request into the deep link
 * DeepLinkHandlerActivity understands. AATL only sees the static inventory; the per-device dynamic
 * shortcuts ("shutdown_<id>") are covered by AssistantFeatureTest. Speech recognition is out of scope.
 */
@RunWith(RobolectricTestRunner.class)
// A plain Application keeps Firebase and the encrypted database out of the test
@Config(sdk = 34, application = Application.class)
public class AppActionsFulfillmentTest {

    private static final String OPEN_APP_FEATURE = "actions.intent.OPEN_APP_FEATURE";

    private AppActionsTestManager aatl;

    @Before
    public void setUp() throws Exception {
        aatl = new AppActionsTestManager(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void openAppFeature_isFulfilledByDeepLinkHandler() {
        Intent intent = fulfill(aatl, "network scan");

        assertEquals("com.ymid.wakeonlan/.appactions.DeepLinkHandlerActivity", intent.getComponent().flattenToShortString());
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("wakeonlan", intent.getData().getScheme());
        assertEquals("action", intent.getData().getHost());
        assertEquals("/feature", intent.getData().getPath());
    }

    @Test
    public void openAppFeature_inventorySynonym_passesShortcutId() {
        Uri data = fulfill(aatl, "find devices").getData();

        assertEquals(AssistantFeature.SCAN_SHORTCUT_ID, data.getQueryParameter("name"));
        assertEquals(AssistantFeature.Kind.OPEN_SCAN, AssistantFeature.parse(data.getQueryParameter("name")).kind);
    }

    @Test
    public void openAppFeature_spanishSynonym_passesShortcutId() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(new Locale("es"));
        AppActionsTestManager spanish = new AppActionsTestManager(context.createConfigurationContext(configuration));

        Uri data = fulfill(spanish, "escaneo de red").getData();

        assertEquals(AssistantFeature.SCAN_SHORTCUT_ID, data.getQueryParameter("name"));
    }

    @Test
    public void openAppFeature_valueOutsideInventory_reachesAppAsPhrase() {
        // AATL passes unmatched values through untouched (the live Assistant only fulfils inventory
        // matches), so the handler must cope: anything that is not a shortcut id is parsed as a phrase
        Uri data = fulfill(aatl, "Apaga Ubuntu").getData();

        assertEquals("Apaga Ubuntu", data.getQueryParameter("name"));
        assertEquals(AssistantFeature.Kind.PHRASE, AssistantFeature.parse(data.getQueryParameter("name")).kind);
    }

    private static Intent fulfill(AppActionsTestManager manager, String feature) {
        AppActionsFulfillmentResult result = manager.fulfill(OPEN_APP_FEATURE, ImmutableMap.of("feature", feature));
        // AATL 1.0.0 reports deep links as INTENT; the url-template shows up as the intent data
        assertEquals(FulfillmentType.INTENT, result.getFulfillmentType());
        Intent intent = ((AppActionsFulfillmentIntentResult) result).getIntent();
        assertNotNull("url-template should produce a deep link", intent.getData());
        return intent;
    }
}
