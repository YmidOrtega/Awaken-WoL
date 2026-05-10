package com.ymid.wakeonlan.appactions;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Strings;

import java.util.List;

import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;

public class DeepLinkHandlerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data == null) {
            finish();
            return;
        }

        // Expected deep link formats:
        // wakeonlan://action/wake?name=My+PC
        // wakeonlan://action/shutdown?name=My+PC
        String path = data.getPath(); // e.g., "/wake"
        String action = null;
        if (path != null && path.length() > 1) {
            action = path.substring(1); // remove leading '/'
        }

        String name = data.getQueryParameter("name");
        if (Strings.isNullOrEmpty(action) || Strings.isNullOrEmpty(name)) {
            Toast.makeText(this, "Invalid deep link", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // find device by name (case-insensitive)
        DeviceRepository repo = DeviceRepository.getInstance(this);
        List<Device> devices = repo.getAll();
        Device matched = null;
        for (Device d : devices) {
            if (d != null && d.name != null && d.name.equalsIgnoreCase(name)) {
                matched = d;
                break;
            }
        }

        if (matched == null) {
            Toast.makeText(this, "Device not found: " + name, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if ("wake".equalsIgnoreCase(action)) {
            AuthenticatedDeviceActionActivity.startWake(this, matched.id);
        } else if ("shutdown".equalsIgnoreCase(action)) {
            AuthenticatedDeviceActionActivity.startShutdown(this, matched.id);
        } else {
            Toast.makeText(this, "Unknown action: " + action, Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
