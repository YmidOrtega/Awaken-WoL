package com.ymid.wakeonlan.ui.backup;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.ui.backup.contracts.ChooseSaveFileDestinationContract;
import com.ymid.wakeonlan.ui.backup.model.DeviceBackupModel;

public class DataExporter implements ActivityResultCallback<Uri> {

    public static final String FILE_MODE_WRITE = "w";

    private final WeakReference<Context> contextWeakReference;
    private final ActivityResultLauncher<Object> activityResultLauncher;

    private boolean includePasswords = false;

    public DataExporter(Fragment fragment) {
        this.contextWeakReference = new WeakReference<>(fragment.getContext());
        activityResultLauncher = fragment.registerForActivityResult(new ChooseSaveFileDestinationContract(), this);
    }

    public void exportDevices(boolean includePasswords) {
        this.includePasswords = includePasswords;
        activityResultLauncher.launch(null);
    }

    @Override
    public void onActivityResult(Uri uri) {
        if (uri == null) {
            return;
        }

        Context context = contextWeakReference.get();
        try {
            List<DeviceBackupModel> devices = DeviceRepository.getInstance(context).getAll()
                    .stream().map(DeviceBackupModel::new)
                    .collect(Collectors.toList());

            if (!includePasswords) {
                devices.forEach(device -> {
                    device.sshPassword = null;
                    device.secureOnPassword = null;
                });
            }

            byte[] content = new Gson().toJson(devices).getBytes(StandardCharsets.UTF_8);
            writeDevicesToFile(uri, content, context);

            Toast.makeText(context, context.getString(R.string.backup_message_export_success, devices.size()), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.backup_message_export_error), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Unable to export devices", e);
        }
    }

    private void writeDevicesToFile(Uri uri, byte[] content, Context context) throws Exception {
        try (OutputStream fileOutputStream = context.getContentResolver().openOutputStream(uri, FILE_MODE_WRITE)) {

            if (fileOutputStream == null) {
                throw new IllegalStateException("Could not open File for writing");
            }

            fileOutputStream.write(content);
        }
    }

}
