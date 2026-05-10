package com.ymid.wakeonlan.shortcuts;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;

public class ShutdownDeviceActivity extends AppCompatActivity {

    public static final String DEVICE_ID_KEY = "deviceId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shutdownDevice();
        finish();
    }

    private void shutdownDevice() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) return;

        int deviceId = extras.getInt(DEVICE_ID_KEY, -1);
        if (deviceId == -1) {
            Toast.makeText(this, R.string.shortcut_wake_device_error, Toast.LENGTH_SHORT).show();
            return;
        }

        AuthenticatedDeviceActionActivity.startShutdown(this, deviceId);
    }
}
