package com.ymid.wakeonlan.ui.modify;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.userauth.UserAuthException;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.databinding.ActivityModifyDeviceBinding;
import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.shutdown.ShutdownModel;
import com.ymid.wakeonlan.shutdown.exception.CommandExecuteException;
import com.ymid.wakeonlan.shutdown.listener.ShutdownExecutorListener;
import com.ymid.wakeonlan.shutdown.test.ShutdownCommandTester;
import com.ymid.wakeonlan.ui.ObfuscatedEditText;
import com.ymid.wakeonlan.ui.modify.watcher.autocomplete.MacAddressAutocomplete;
import com.ymid.wakeonlan.ui.modify.watcher.validator.ConditionalInputNotEmptyValidator;
import com.ymid.wakeonlan.ui.modify.watcher.validator.InputNotEmptyValidator;
import com.ymid.wakeonlan.ui.modify.watcher.validator.MacValidator;
import com.ymid.wakeonlan.ui.modify.watcher.validator.PortValidator;
import com.ymid.wakeonlan.ui.modify.watcher.validator.SecureOnPasswordValidator;

public abstract class ModifyDeviceActivity extends AppCompatActivity {

    protected ActivityModifyDeviceBinding binding;
    protected DeviceRepository deviceRepository;

    protected ObfuscatedEditText deviceMacInput;
    protected TextInputEditText deviceNameInput;
    protected ObfuscatedEditText deviceStatusIpInput;
    protected ObfuscatedEditText deviceBroadcastInput;
    protected TextInputEditText deviceSecureOnPassword;
    protected ImageButton broadcastAutofill;
    protected TextInputEditText devicePorts;
    protected ConstraintLayout deviceRemoteShutdownContainer;
    protected SwitchCompat deviceEnableRemoteShutdown;
    protected ObfuscatedEditText deviceSshAddressInput;
    protected TextInputEditText deviceSshPortInput;
    protected TextInputEditText deviceSshUsernameInput;
    protected TextInputEditText deviceSshPasswordInput;
    protected TextInputEditText deviceSshCommandInput;
    protected Spinner deviceSshOsSpinner;
    protected TextView deviceSshOsSuggestion;
    protected Button sshTestShutdownButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityModifyDeviceBinding.inflate(getLayoutInflater());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(binding.getRoot());

        devicePorts = binding.device.devicePorts;
        deviceMacInput = binding.device.deviceMac;
        deviceNameInput = binding.device.deviceName;
        deviceStatusIpInput = binding.device.deviceStatusIp;
        deviceBroadcastInput = binding.device.deviceBroadcast;
        deviceSecureOnPassword = binding.device.deviceSecureOnPassword;
        broadcastAutofill = binding.device.broadcastAutofill;

        deviceRemoteShutdownContainer = binding.device.deviceRemoteShutdownContainer;
        deviceEnableRemoteShutdown = binding.device.deviceSwitchRemoteShutdown;
        deviceSshAddressInput = binding.device.deviceShutdownAddress;
        deviceSshPortInput = binding.device.deviceShutdownPort;
        deviceSshUsernameInput = binding.device.deviceShutdownUsername;
        deviceSshPasswordInput = binding.device.deviceShutdownPassword;
        deviceSshCommandInput = binding.device.deviceShutdownCommand;
        deviceSshOsSpinner = binding.device.deviceShutdownOs;
        deviceSshOsSuggestion = binding.device.deviceShutdownOsSuggestion;

        sshTestShutdownButton = binding.device.deviceButtonTestShutdown;

        setSupportActionBar(binding.toolbar);

        // Intercept insets at the CoordinatorLayout root so its fitsSystemWindows handling
        // never offsets the AppBarLayout below the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            binding.appBarLayout.setPadding(
                    binding.appBarLayout.getPaddingLeft(), topInset,
                    binding.appBarLayout.getPaddingRight(), binding.appBarLayout.getPaddingBottom());
            ViewCompat.dispatchApplyWindowInsets(binding.device.getRoot(), insets);
            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.device.getRoot(), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int actionBarSize = resolveActionBarSize();
            int imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            // Use the larger of keyboard height or navigation bar so the ScrollView always
            // has enough room to scroll the focused field above the keyboard.
            int bottomPadding = Math.max(imeInset, navInset);
            v.setPadding(v.getPaddingLeft(), actionBarSize + topInset, v.getPaddingRight(), bottomPadding);
            return insets;
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);

        deviceRepository = DeviceRepository.getInstance(this);
        addValidators();
        addAutofillClickHandler();
        setRemoteDeviceShutdownSwitchListener();
        setOsSpinnerSuggestionListener();
        setOnTestSshShutdownListenerClickedListener();
    }

    private void setOsSpinnerSuggestionListener() {
        deviceSshOsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String os = getSelectedOs(deviceSshOsSpinner);
                String suggested = getSuggestedCommandForOs(os);
                deviceSshOsSuggestion.setText(getString(R.string.add_device_shutdown_os_suggestion, suggested));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                deviceSshOsSuggestion.setText("");
            }
        });
    }

    private String getSuggestedCommandForOs(String os) {
        switch (os) {
            case "windows":
                return "shutdown /s /t 0";
            case "macos":
                return "sudo shutdown -h now";
            default: // linux
                return "sudo shutdown -h now";
        }
    }

    private void addAutofillClickHandler() {
        broadcastAutofill.setOnClickListener(v -> {
            Optional<InetAddress> broadcastAddress = new BroadcastHelper().getBroadcastAddress();
            broadcastAddress.ifPresent(inetAddress -> deviceBroadcastInput.setText(inetAddress.getHostAddress()));
        });
    }

    private void setRemoteDeviceShutdownSwitchListener() {
        deviceEnableRemoteShutdown.setOnCheckedChangeListener((buttonView, isChecked) -> triggerRemoteShutdownLayoutVisibility(isChecked));
    }

    protected void triggerRemoteShutdownLayoutVisibility(boolean isEnabled) {
        deviceRemoteShutdownContainer.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }

    private void addValidators() {
        deviceMacInput.addTextChangedListener(new MacValidator(deviceMacInput));
        deviceMacInput.addTextChangedListener(new MacAddressAutocomplete());

        devicePorts.addTextChangedListener(new PortValidator(devicePorts));

        deviceNameInput.addTextChangedListener(new InputNotEmptyValidator(deviceNameInput, R.string.add_device_error_name_empty));
        deviceSecureOnPassword.addTextChangedListener(new SecureOnPasswordValidator(deviceSecureOnPassword));

        List<Supplier<Boolean>> remoteShutdownEnabledSupplier = Collections.singletonList(() -> deviceEnableRemoteShutdown.isChecked());
        List<Supplier<Boolean>> statusIpFallbackAvailable =
                Lists.newArrayList(() -> deviceEnableRemoteShutdown.isChecked(), () -> isEmpty(deviceStatusIpInput));

        deviceSshAddressInput.addTextChangedListener(new ConditionalInputNotEmptyValidator(deviceSshAddressInput,
                R.string.add_device_error_ssh_address_empty, statusIpFallbackAvailable));
        deviceSshUsernameInput.addTextChangedListener(new ConditionalInputNotEmptyValidator(deviceSshUsernameInput,
                R.string.add_device_error_ssh_username_empty, remoteShutdownEnabledSupplier));
        deviceSshPasswordInput.addTextChangedListener(new ConditionalInputNotEmptyValidator(deviceSshPasswordInput,
                R.string.add_device_error_ssh_password_empty, remoteShutdownEnabledSupplier));
        deviceSshCommandInput.addTextChangedListener(new ConditionalInputNotEmptyValidator(deviceSshCommandInput,
                R.string.add_device_error_ssh_command_empty, remoteShutdownEnabledSupplier));
    }

    protected boolean assertInputsNotEmptyAndValid() {
        return deviceMacInput.getError() == null && isNotEmpty(deviceMacInput) &&
                devicePorts.getError() == null &&
                deviceNameInput.getError() == null && isNotEmpty(deviceNameInput) &&
                deviceStatusIpInput.getError() == null &&
                deviceSecureOnPassword.getError() == null &&
                deviceSshAddressInput.getError() == null &&
                deviceSshUsernameInput.getError() == null &&
                deviceSshPasswordInput.getError() == null &&
                deviceSshCommandInput.getError() == null;
    }

    protected boolean assertShutdownInputsNotEmptyAndValid() {
        return deviceEnableRemoteShutdown.isChecked() &&
                deviceStatusIpInput.getError() == null &&
                deviceSshAddressInput.getError() == null &&
                deviceSshUsernameInput.getError() == null &&
                deviceSshPasswordInput.getError() == null &&
                deviceSshCommandInput.getError() == null;
    }

    private boolean isNotEmpty(TextInputEditText inputEditText) {
        return !isEmpty(inputEditText);
    }

    private boolean isEmpty(TextInputEditText inputEditText) {
        return inputEditText.getText() == null || inputEditText.getText().length() == 0;
    }

    private boolean isNotEmpty(ObfuscatedEditText inputEditText) {
        return !isEmpty(inputEditText);
    }

    private boolean isEmpty(ObfuscatedEditText inputEditText) {
        String text = inputEditText.getRealTextValue();
        return text == null || text.length() == 0;
    }

    @NonNull
    protected String getSelectedOs(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItem() == null) {
            return "linux";
        }
        String selected = spinner.getSelectedItem().toString().toLowerCase();
        if (selected.contains("linux")) {
            return "linux";
        } else if (selected.contains("windows")) {
            return "windows";
        } else if (selected.contains("mac")) {
            return "macos";
        }
        return "linux";
    }

    protected void checkAndPersistDevice() {
        triggerValidators();
        if (assertInputsNotEmptyAndValid()) {
            persistDevice(buildDeviceFromInputs());
            finish();
        } else {
            Toast.makeText(this, R.string.add_device_error_save_clicked, Toast.LENGTH_LONG).show();
        }
    }

    private int resolveActionBarSize() {
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());
        }
        return getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
    }

    private void triggerValidators() {
        // ensure spinner value is touched so data binding updates if needed
        if (deviceSshOsSpinner != null && deviceSshOsSpinner.getSelectedItem() == null) {
            deviceSshOsSpinner.setSelection(0);
        }
        deviceNameInput.setText(deviceNameInput.getText());
        triggerObfuscatedInputValidators(deviceStatusIpInput);
        triggerObfuscatedInputValidators(deviceBroadcastInput);
        triggerObfuscatedInputValidators(deviceMacInput);
        devicePorts.setText(devicePorts.getText());
        deviceSecureOnPassword.setText(deviceSecureOnPassword.getText());
        triggerShutdownValidators();
    }

    private void triggerShutdownValidators() {
        triggerObfuscatedInputValidators(deviceStatusIpInput);
        triggerObfuscatedInputValidators(deviceSshAddressInput);
        deviceSshUsernameInput.setText(deviceSshUsernameInput.getText());
        deviceSshPasswordInput.setText(deviceSshPasswordInput.getText());
        deviceSshCommandInput.setText(deviceSshCommandInput.getText());
    }

    private void triggerObfuscatedInputValidators(ObfuscatedEditText input) {
        input.setRealText(input.getRealTextValue());
    }

    private void setOnTestSshShutdownListenerClickedListener() {
        sshTestShutdownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerShutdownValidators();
                if (assertShutdownInputsNotEmptyAndValid()) {
                    Device device = buildDeviceFromInputs();

                    View view = LayoutInflater.from(ModifyDeviceActivity.this).inflate(R.layout.dialog_test_remote_shutdown, null);
                    AlertDialog dialog = new MaterialAlertDialogBuilder(ModifyDeviceActivity.this)
                            .setView(view)
                            .setTitle(R.string.remote_shutdown_send_command_dialog_title)
                            .setPositiveButton(android.R.string.ok, (dlg, which) -> dlg.dismiss())
                            .create();

                    final LinearLayout destinationReachedResult = view.findViewById(R.id.result_destination_reached);
                    final LinearLayout authorizationResult = view.findViewById(R.id.result_authorization);
                    final LinearLayout sessionCreatedResult = view.findViewById(R.id.result_session);
                    final LinearLayout commandExecutedResult = view.findViewById(R.id.result_command_execute);

                    final TextView optionalErrorMessage = view.findViewById(R.id.result_optional_error_message);
                    optionalErrorMessage.setVisibility(View.GONE);

                    setInitialDialogTexts(destinationReachedResult, authorizationResult, sessionCreatedResult, commandExecutedResult);

                    new ShutdownCommandTester(new ShutdownExecutorListener() {
                        @Override
                        public void onTargetHostReached() {
                            setStepSuccessfullyCompleted(destinationReachedResult, R.string.test_shutdown_successful_destination);
                        }

                        @Override
                        public void onLoginSuccessful() {
                            setStepSuccessfullyCompleted(authorizationResult, R.string.test_shutdown_successful_authorization);
                        }

                        @Override
                        public void onSessionStartSuccessful() {
                            setStepSuccessfullyCompleted(sessionCreatedResult, R.string.test_shutdown_successful_session);
                        }

                        @Override
                        public void onCommandExecuteSuccessful() {
                            setStepSuccessfullyCompleted(commandExecutedResult, R.string.test_shutdown_successful_command_execute);
                        }

                        @Override
                        public void onSudoPromptTriggered(ShutdownModel shutdownModel) {
                            runOnUiThread(() -> {
                                optionalErrorMessage.setVisibility(View.VISIBLE);
                                optionalErrorMessage.setText(getString(R.string.test_shutdown_error_execution_sudo_prompt, shutdownModel.getCommand()));
                            });
                        }

                        @Override
                        public void onDangerousCommandDetected(ShutdownModel shutdownModel) {
                            runOnUiThread(() -> {
                                optionalErrorMessage.setVisibility(View.VISIBLE);
                                optionalErrorMessage.setText(getString(R.string.test_shutdown_error_dangerous_command, shutdownModel.getCommand()));
                            });
                        }

                        @Override
                        public void onGeneralError(Exception exception, ShutdownModel shutdownModel) {
                            runOnUiThread(() -> {
                                optionalErrorMessage.setVisibility(View.VISIBLE);
                                optionalErrorMessage.setText(getTextByExceptionType(exception, shutdownModel));
                            });
                        }

                    }).startShutdownCommandTest(device, getSelectedOs(deviceSshOsSpinner));

                    dialog.show();
                } else {
                    Toast.makeText(ModifyDeviceActivity.this, R.string.add_device_error_save_clicked, Toast.LENGTH_LONG).show();
                }
            }

            private String getTextByExceptionType(Exception exception, ShutdownModel shutdownModel) {
                if (exception instanceof ConnectException) {
                    return getString(R.string.test_shutdown_error_connect_exception, shutdownModel.getSshAddress(), shutdownModel.getSshPort());
                } else if (exception instanceof UnknownHostException) {
                    return getString(R.string.test_shutdown_error_unknown_host, shutdownModel.getSshAddress());
                } else if (exception instanceof UserAuthException) {
                    return getString(R.string.test_shutdown_error_auth_exception, shutdownModel.getUsername(), shutdownModel.getSshAddress());
                } else if (exception instanceof ConnectionException && Throwables.getRootCause(exception) instanceof TimeoutException) {
                    return getString(R.string.test_shutdown_error_execution_timeout, shutdownModel.getCommand());
                } else if (exception instanceof CommandExecuteException) {
                    Integer exitStatus = ((CommandExecuteException) exception).getExitStatus();
                    String explanationString = getExitCodeExplanationStringRes(exitStatus);
                    return getString(R.string.test_shutdown_error_execution_exception, shutdownModel.getCommand(), exitStatus, explanationString);
                }

                return getString(R.string.test_shutdown_error_unknown_exception, exception.getMessage());
            }

            private String getExitCodeExplanationStringRes(Integer exitStatus) {
                switch (exitStatus) {
                    case 127:
                        return getString(R.string.execution_error_command_not_found);
                    case 126:
                        return getString(R.string.execution_error_command_not_executable);
                    default:
                        return getString(R.string.execution_error_unknown);
                }
            }

            private void runOnUiThread(Runnable runnable) {
                ModifyDeviceActivity.this.runOnUiThread(runnable);
            }

            private void setStepSuccessfullyCompleted(LinearLayout layout, int stringResourceId) {
                runOnUiThread(() -> {
                    TextView resultMessage = getResultMessageView(layout);
                    RadioButton resultIndicator = getResultRadioButton(layout);

                    resultMessage.setText(stringResourceId);
                    resultIndicator.setChecked(true);
                    resultIndicator.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#479c44")));
                });
            }
        });
    }

    private void setInitialDialogTexts(LinearLayout destinationReachedResult, LinearLayout authorizationResult,
                                       LinearLayout sessionCreatedResult, LinearLayout commandExecutedResult) {
        setTexts(getResultMessageView(destinationReachedResult), R.string.test_shutdown_initial_destination);
        setTexts(getResultMessageView(authorizationResult), R.string.test_shutdown_initial_authorization);
        setTexts(getResultMessageView(sessionCreatedResult), R.string.test_shutdown_initial_session);
        setTexts(getResultMessageView(commandExecutedResult), R.string.test_shutdown_initial_command_execute);
    }

    private void setTexts(TextView resultMessageView, int stringResourceId) {
        resultMessageView.setText(stringResourceId);
    }

    private RadioButton getResultRadioButton(LinearLayout layout) {
        return layout.findViewById(R.id.test_shutdown_item_radio);
    }

    private TextView getResultMessageView(LinearLayout layout) {
        return layout.findViewById(R.id.test_shutdown_item_result_message);
    }

    abstract protected void persistDevice(Device device);

    abstract protected Device buildDeviceFromInputs();

    abstract protected boolean inputsHaveNotChanged();

    protected int getPort() {
        try {
            String wakePort = getInputText(devicePorts);
            if (Strings.nullToEmpty(wakePort).isEmpty()) {
                return 9;
            }
            return Integer.parseInt(wakePort);
        } catch (NumberFormatException e) {
            return 9;
        }
    }

    @NonNull
    private String getInputText(TextInputEditText testInput) {
        return testInput.getText() != null ? testInput.getText().toString().trim() : "";
    }

    @NonNull
    private String getInputText(ObfuscatedEditText testInput) {
        return testInput.getRealTextValue() != null ? testInput.getRealTextValue().trim() : "";
    }

    @NonNull
    protected String getDeviceBroadcastAddressText() {
        return getInputText(deviceBroadcastInput);
    }

    @NonNull
    protected String getDeviceMacInputText() {
        return getInputText(deviceMacInput);
    }

    @NonNull
    protected String getDeviceNameInputText() {
        return getInputText(deviceNameInput);
    }

    @NonNull
    protected String getDeviceStatusIpText() {
        return getInputText(deviceStatusIpInput);
    }

    @NonNull
    protected String getDeviceSecureOnPassword() {
        return getInputText(deviceSecureOnPassword);
    }

    protected boolean getDeviceRemoteShutdownEnabled() {
        return deviceEnableRemoteShutdown.isChecked();
    }

    @NonNull
    protected String getDeviceSshAddress() {
        return getInputText(deviceSshAddressInput);
    }

    @NonNull
    protected Integer getDeviceSshPort() {
        try {
            String sshPort = getInputText(deviceSshPortInput);
            if (Strings.nullToEmpty(sshPort).isEmpty()) {
                return -1;
            }
            return Integer.parseInt(sshPort);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @NonNull
    protected String getDeviceSshUsername() {
        return getInputText(deviceSshUsernameInput);
    }

    @NonNull
    protected String getDeviceSshPassword() {
        return getInputText(deviceSshPasswordInput);
    }

    @NonNull
    protected String getDeviceSshCommand() {
        return getInputText(deviceSshCommandInput);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (inputsHaveNotChanged()) {
            finish();
            return false;
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.modify_device_unsaved_changes_title)
                    .setMessage(R.string.modify_device_unsaved_changes_message)
                    .setPositiveButton(R.string.modify_device_unsaved_changes_positive, (dialog, which) -> checkAndPersistDevice())
                    .setNegativeButton(R.string.modify_device_unsaved_changes_negative, (dialog, which) -> finish())
                    .create().show();
        }

        return false;
    }
}
