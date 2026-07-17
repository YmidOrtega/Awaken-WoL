package com.ymid.wakeonlan.ui.backup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.databinding.FragmentBackupBinding;

public class BackupFragment extends Fragment {

    private DataExporter dataExporter;
    private DataImporter dataImporter;

    private FragmentBackupBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataExporter = new DataExporter(this);
        dataImporter = new DataImporter(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBackupBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonExport.setOnClickListener(v -> showExportDialog());
        binding.buttonImport.setOnClickListener(v -> dataImporter.importDevices());
    }

    private void showExportDialog() {
        boolean[] includePasswords = {false};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.backup_export_dialog_title)
                .setMultiChoiceItems(
                        new CharSequence[]{getString(R.string.backup_export_include_passwords)},
                        new boolean[]{false},
                        (dialog, which, isChecked) -> includePasswords[0] = isChecked)
                .setPositiveButton(R.string.backup_button_export,
                        (dialog, which) -> dataExporter.exportDevices(includePasswords[0]))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

}
