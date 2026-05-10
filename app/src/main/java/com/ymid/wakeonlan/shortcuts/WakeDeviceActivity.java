package com.ymid.wakeonlan.shortcuts;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;

public class WakeDeviceActivity extends AppCompatActivity {

    public static final String DEVICE_ID_KEY = "deviceId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wakeDevice();
        finish();
    }

    private void wakeDevice() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int machineId = extras.getInt(DEVICE_ID_KEY, -1);
            if (machineId == -1) {
                Toast.makeText(this, R.string.shortcut_wake_device_error, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            AuthenticatedDeviceActionActivity.startWake(this, machineId);
        }
    }

}
