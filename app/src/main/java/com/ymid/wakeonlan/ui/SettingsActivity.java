package com.ymid.wakeonlan.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ymid.wakeonlan.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(findViewById(android.R.id.content));
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements TrustedWifiDialogFragment.Callback {

        private static final String PREF_TRUSTED_SSIDS = "pref_trusted_ssids";
        private static final String PREF_SCAN_WIFI = "pref_scan_wifi";

        private ActivityResultLauncher<String[]> wifiPermissionLauncher;
        private WifiManager wifiManager;
        private Preference trustedSsidsPreference;
        private Preference scanWifiPreference;
        private boolean openDialogAfterScan;
        private boolean receiverRegistered;
        private final Handler handler = new Handler(Looper.getMainLooper());

        // Store scanned entries for later use
        private String[] scannedEntries = new String[0];
        private String[] scannedValues = new String[0];

        private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWifiScanResults();
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            wifiPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean granted = true;
                        for (Boolean value : result.values()) {
                            if (!Boolean.TRUE.equals(value)) {
                                granted = false;
                                break;
                            }
                        }

                        if (granted) {
                            startWifiScanWithPermission(true);
                        } else {
                            Toast.makeText(requireContext(), R.string.wifi_scan_permission_denied, Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            trustedSsidsPreference = findPreference(PREF_TRUSTED_SSIDS);
            scanWifiPreference = findPreference(PREF_SCAN_WIFI);

            if (trustedSsidsPreference != null) {
                updateTrustedSsidsSummary();

                trustedSsidsPreference.setOnPreferenceClickListener(preference -> {
                    // Always show custom dialog
                    showTrustedWifiDialog();
                    return true;
                });
            }

            if (scanWifiPreference != null) {
                scanWifiPreference.setOnPreferenceClickListener(preference -> {
                    startWifiScanWithPermission(true); // Explicitly requested
                    return true;
                });
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            ensureReceiverRegistered();
        }

        @Override
        public void onStop() {
            super.onStop();
            unregisterReceiverIfNeeded();
        }

        private void startWifiScanWithPermission(boolean fromUser) {
            if (wifiManager == null || !wifiManager.isWifiEnabled()) {
                if (fromUser) Toast.makeText(requireContext(), R.string.wifi_scan_disabled, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isLocationEnabled()) {
                if (fromUser) Toast.makeText(requireContext(), R.string.wifi_scan_location_disabled, Toast.LENGTH_SHORT).show();
                return;
            }

            if (hasWifiScanPermission()) {
                ensureReceiverRegistered();
                startWifiScan(fromUser);
                return;
            }

            if (fromUser) {
                wifiPermissionLauncher.launch(getWifiScanPermissions());
            }
        }

        private void startWifiScan(boolean fromUser) {
            if (wifiManager == null) {
                if (fromUser) Toast.makeText(requireContext(), R.string.wifi_scan_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            openDialogAfterScan = fromUser;
            boolean started = wifiManager.startScan();
            if (!started) {
                openDialogAfterScan = false;
                if (fromUser) {
                    Toast.makeText(requireContext(), R.string.wifi_scan_failed, Toast.LENGTH_SHORT).show();
                    updateWifiScanResults();
                }
                return;
            }

            if (fromUser) {
                handler.postDelayed(this::updateWifiScanResults, 2000);
            }
        }

        private void updateWifiScanResults() {
            if (!isAdded() || wifiManager == null || trustedSsidsPreference == null) {
                return;
            }

            List<ScanResult> results = wifiManager.getScanResults();
            Map<String, ScanResult> bestResults = new HashMap<>();
            for (ScanResult result : results) {
                if (TextUtils.isEmpty(result.SSID)) {
                    continue;
                }
                ScanResult currentBest = bestResults.get(result.SSID);
                if (currentBest == null || result.level > currentBest.level) {
                    bestResults.put(result.SSID, result);
                }
            }

            List<ScanResult> sortedResults = new ArrayList<>(bestResults.values());
            Collections.sort(sortedResults, (left, right) -> {
                int levelCompare = Integer.compare(right.level, left.level);
                if (levelCompare != 0) {
                    return levelCompare;
                }
                return left.SSID.compareToIgnoreCase(right.SSID);
            });

            String[] entries = new String[sortedResults.size()];
            String[] entryValues = new String[sortedResults.size()];
            for (int i = 0; i < sortedResults.size(); i++) {
                ScanResult result = sortedResults.get(i);
                entryValues[i] = result.SSID;
                entries[i] = formatWifiEntry(result);
            }

            scannedEntries = entries;
            scannedValues = entryValues;

            if (openDialogAfterScan) {
                openDialogAfterScan = false;
                if (sortedResults.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.wifi_scan_empty, Toast.LENGTH_SHORT).show();
                } else {
                    showTrustedWifiDialogScanMode();
                }
            }
        }

        private boolean hasWifiScanPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.NEARBY_WIFI_DEVICES)
                        == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
            }
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }

        private String[] getWifiScanPermissions() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return new String[]{Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION};
            }
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }

        private void ensureReceiverRegistered() {
            if (!receiverRegistered && isAdded()) {
                ContextCompat.registerReceiver(requireContext(), wifiScanReceiver,
                        new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                        ContextCompat.RECEIVER_NOT_EXPORTED);
                receiverRegistered = true;
            }
        }

        private void unregisterReceiverIfNeeded() {
            if (receiverRegistered) {
                requireContext().unregisterReceiver(wifiScanReceiver);
                receiverRegistered = false;
            }
            handler.removeCallbacksAndMessages(null);
        }

        private boolean isLocationEnabled() {
            LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return locationManager.isLocationEnabled();
            }
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }

        @Override
        public void onTrustedWifiSelection(Set<String> selected) {
            if (trustedSsidsPreference != null) {
                // Save to SharedPreferences
                getPreferenceManager().getSharedPreferences()
                        .edit()
                        .putStringSet(PREF_TRUSTED_SSIDS, selected)
                        .apply();
                updateTrustedSsidsSummary();
            }
        }

        private void updateTrustedSsidsSummary() {
            if (trustedSsidsPreference == null) {
                return;
            }
            Set<String> values = getPreferenceManager().getSharedPreferences()
                    .getStringSet(PREF_TRUSTED_SSIDS, null);
            if (values == null || values.isEmpty()) {
                trustedSsidsPreference.setSummary(getString(R.string.pref_trusted_ssids_summary_empty));
            } else {
                trustedSsidsPreference.setSummary(TextUtils.join(", ", values));
            }
        }

        private Set<String> getTrustedSsids() {
            return getPreferenceManager().getSharedPreferences()
                    .getStringSet(PREF_TRUSTED_SSIDS, new HashSet<>());
        }

        private void showTrustedWifiDialog() {
            // MODE_VIEW: Show only saved networks with delete buttons
            if (trustedSsidsPreference == null) {
                return;
            }

            Set<String> selected = getTrustedSsids();

            TrustedWifiDialogFragment dialog = TrustedWifiDialogFragment.newInstance(
                    new String[0],
                    new String[0],
                    new ArrayList<>(selected),
                    TrustedWifiDialogFragment.MODE_VIEW
            );
            dialog.show(getChildFragmentManager(), "trustedWifiDialog");
        }

        private void showTrustedWifiDialogScanMode() {
            // MODE_SCAN: Show all scanned networks with checkboxes
            if (trustedSsidsPreference == null) {
                return;
            }

            Set<String> selected = getTrustedSsids();

            TrustedWifiDialogFragment dialog = TrustedWifiDialogFragment.newInstance(
                    scannedEntries,
                    scannedValues,
                    new ArrayList<>(selected),
                    TrustedWifiDialogFragment.MODE_SCAN
            );
            dialog.show(getChildFragmentManager(), "trustedWifiDialog");
        }


        private String formatWifiEntry(ScanResult result) {
            String band = formatWifiBand(result.frequency);
            StringBuilder details = new StringBuilder();
            if (!TextUtils.isEmpty(band)) {
                details.append(band);
            }
            details.append(details.length() > 0 ? ", " : "")
                    .append(result.level)
                    .append(" dBm");
            return result.SSID + " (" + details + ")";
        }

        private String formatWifiBand(int frequency) {
            if (frequency >= 2400 && frequency < 2500) {
                return "2.4 GHz";
            }
            if (frequency >= 4900 && frequency < 5900) {
                return "5 GHz";
            }
            if (frequency >= 5925 && frequency < 7125) {
                return "6 GHz";
            }
            return "";
        }
    }
}
