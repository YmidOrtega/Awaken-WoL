package com.ymid.wakeonlan.security;

import android.app.KeyguardManager;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.common.base.Strings;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.entities.ActionType;
import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.ActionLogRepository;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.shutdown.ShutdownExecutor;
import com.ymid.wakeonlan.shutdown.listener.IgnoringShutdownExecutorListener;
import com.ymid.wakeonlan.shutdown.ShutdownModelFactory;
import com.ymid.wakeonlan.ui.modify.EditDeviceActivity;
import com.ymid.wakeonlan.ui.notifications.NotificationHelper;
import com.ymid.wakeonlan.wol.WolSender;

public class AuthenticatedDeviceActionActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_ID = "deviceId";
    public static final String EXTRA_DEVICE_ACTION = "deviceAction";

    public static final String ACTION_WAKE = "wake";
    public static final String ACTION_SHUTDOWN = "shutdown";
    public static final String ACTION_EDIT = "edit";

    private static final int AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG
            | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    public static void startWake(Context context, int deviceId) {
        start(context, deviceId, ACTION_WAKE);
    }

    public static void startShutdown(Context context, int deviceId) {
        start(context, deviceId, ACTION_SHUTDOWN);
    }

    public static void startEdit(Context context, int deviceId) {
        start(context, deviceId, ACTION_EDIT);
    }

    private static void start(Context context, int deviceId, String action) {
        Intent intent = new Intent(context, AuthenticatedDeviceActionActivity.class);
        intent.putExtra(EXTRA_DEVICE_ID, deviceId);
        intent.putExtra(EXTRA_DEVICE_ACTION, action);
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Device device = resolveDevice();
        String action = getIntent().getStringExtra(EXTRA_DEVICE_ACTION);
        if (device == null || Strings.isNullOrEmpty(action)) {
            showErrorAndFinish();
            return;
        }

        // Check app setting: if authentication is disabled, execute directly
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean requireAuth = prefs.getBoolean("pref_require_auth", true);
        if (!ACTION_EDIT.equals(action)) {
            if (!requireAuth || shouldSkipAuthentication(prefs)) {
                executeDeviceAction(device, action);
                finish();
                return;
            }
        }

        authenticate(device, action);
    }

    private Device resolveDevice() {
        int deviceId = getIntent().getIntExtra(EXTRA_DEVICE_ID, -1);
        if (deviceId == -1) {
            return null;
        }
        return DeviceRepository.getInstance(this).getById(deviceId);
    }

    private void authenticate(Device device, String action) {
        if (!canAuthenticate()) {
            Toast.makeText(this, R.string.auth_device_action_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                executeDeviceAction(device, action);
                finish();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(AuthenticatedDeviceActionActivity.this, R.string.auth_device_action_failed, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(AuthenticatedDeviceActionActivity.this, R.string.auth_device_action_failed, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo.Builder promptBuilder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.auth_device_action_title))
                .setSubtitle(getSubtitle(device, action));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptBuilder.setAllowedAuthenticators(AUTHENTICATORS);
        } else {
            promptBuilder.setDeviceCredentialAllowed(true);
        }

        biometricPrompt.authenticate(promptBuilder.build());
    }

    private boolean canAuthenticate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return BiometricManager.from(this).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS;
        }

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager != null && keyguardManager.isDeviceSecure();
    }

    private String getSubtitle(Device device, String action) {
        if (ACTION_EDIT.equals(action)) {
            return getString(R.string.auth_edit_device_subtitle, device.name);
        }
        if (ACTION_SHUTDOWN.equals(action)) {
            return getString(R.string.auth_shutdown_device_subtitle, device.name);
        }
        return getString(R.string.auth_wake_device_subtitle, device.name);
    }

    private void executeDeviceAction(Device device, String action) {
        if (ACTION_EDIT.equals(action)) {
            Intent intent = new Intent(this, EditDeviceActivity.class);
            Bundle bundle = new Bundle();
            bundle.putParcelable(EditDeviceActivity.DEVICE_PARCELABLE_KEY, device);
            intent.putExtras(bundle);
            startActivity(intent);
            return;
        }
        if (ACTION_SHUTDOWN.equals(action)) {
            if (!ShutdownModelFactory.fromDevice(device).isPresent()) {
                showActionError();
                return;
            }
            ShutdownExecutor.shutdownDevice(device, device.shutdownOs == null ? "linux" : device.shutdownOs, new IgnoringShutdownExecutorListener());
            ActionLogRepository.getInstance(this).log(device.name, ActionType.SHUTDOWN);
            Toast.makeText(this, getString(R.string.remote_shutdown_send_command, device.name), Toast.LENGTH_LONG).show();
            NotificationHelper.INSTANCE.sendShutdownSentNotification(this, device.name);
            return;
        }

        if (!ACTION_WAKE.equals(action) || Strings.isNullOrEmpty(device.macAddress)) {
            showActionError();
            return;
        }

        WolSender.sendWolPacket(device);
        ActionLogRepository.getInstance(this).log(device.name, ActionType.WAKE);
        Toast.makeText(this, getString(R.string.wol_toast_sending_packet, device.name), Toast.LENGTH_LONG).show();
        NotificationHelper.INSTANCE.sendWakeSentNotification(this, device.name);
    }

    private void showActionError() {
        Toast.makeText(this, R.string.auth_device_action_error, Toast.LENGTH_SHORT).show();
    }

    private boolean shouldSkipAuthentication(SharedPreferences prefs) {
        Set<String> trustedSsids = prefs.getStringSet("pref_trusted_ssids", Collections.emptySet());
        if (trustedSsids == null || trustedSsids.isEmpty()) {
            return false;
        }

        String currentSsid = getCurrentWifiSsid();
        if (Strings.isNullOrEmpty(currentSsid)) {
            return false;
        }

        return trustedSsids.contains(currentSsid);
    }

    private String getCurrentWifiSsid() {
        if (!hasWifiSsidPermission()) {
            return null;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (connectivityManager == null || wifiManager == null) {
            return null;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return null;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return null;
        }

        String ssid = wifiInfo.getSSID();
        if (ssid == null) {
            return null;
        }

        if ("<unknown ssid>".equalsIgnoreCase(ssid)) {
            return null;
        }

        if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() > 1) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        return ssid;
    }

    private boolean hasWifiSsidPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.NEARBY_WIFI_DEVICES)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showErrorAndFinish() {
        showActionError();
        finish();
    }
}
