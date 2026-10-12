package com.ymid.wakeonlan.ui.list;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.databinding.FragmentListDevicesBinding;
import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.rating.RatingHelper;
import com.ymid.wakeonlan.security.AuthenticatedDeviceActionActivity;
import com.ymid.wakeonlan.shutdown.ShutdownModelFactory;
import com.ymid.wakeonlan.ui.ConsoleHeaderHost;
import com.ymid.wakeonlan.ui.list.layoutmanager.GridLayoutManagerWrapper;
import com.ymid.wakeonlan.ui.list.layoutmanager.LinearLayoutManagerWrapper;
import com.ymid.wakeonlan.ui.list.status.DeviceStatusBoard;
import com.ymid.wakeonlan.ui.list.status.pool.PingStatusTesterPool;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTestType;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTesterPool;


public class DeviceListFragment extends Fragment {

    private DeviceRepository deviceRepository;
    private FragmentListDevicesBinding binding;
    private DeviceListAdapter deviceListAdapter;
    private List<Device> devices = new ArrayList<>();

    private static final StatusTesterPool STATUS_TESTER_POOL = PingStatusTesterPool.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentListDevicesBinding.inflate(inflater, container, false);
        binding.addDeviceButton.setOnClickListener(view -> openAddDevice());
        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        PingStatusTesterPool.getInstance().pauseAllForType(StatusTestType.LIST);
    }

    @Override
    public void onResume() {
        super.onResume();
        PingStatusTesterPool.getInstance().resumeAll();
        updateHeader();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        deviceRepository = DeviceRepository.getInstance(getContext());

        instantiateRecyclerView();
        initializeCommandBar();
        registerLiveDataObserver();
    }

    private void registerLiveDataObserver() {
        deviceRepository.getAllAsObservable()
                .observe(getViewLifecycleOwner(), devices -> {
                    this.devices = devices == null ? new ArrayList<>() : devices;
                    deviceListAdapter.updateDataset(devices);
                    updateCommandSuggestions();
                    updateHeader();
                });
        DeviceStatusBoard.getInstance().getChanges()
                .observe(getViewLifecycleOwner(), change -> updateHeader());
    }

    private void updateHeader() {
        if (!(getActivity() instanceof ConsoleHeaderHost) || !isResumed()) {
            return;
        }
        ConsoleHeaderHost host = (ConsoleHeaderHost) getActivity();
        String subnet = LocalSubnet.describe(requireContext());
        String network = subnet == null ? getString(R.string.header_no_network) : getString(R.string.header_network, subnet);
        String count = getResources().getQuantityString(R.plurals.header_device_count, devices.size(), devices.size());
        host.setHeaderSubtitle(network + " · " + count);

        if (devices.isEmpty()) {
            host.setHeaderCounter(null);
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (Device device : devices) {
            ids.add(device.id);
        }
        int online = DeviceStatusBoard.getInstance().countOnline(ids);
        host.setHeaderCounter(online + "/" + devices.size());
    }

    private void initializeCommandBar() {
        binding.commandInput.setOnEditorActionListener((view, actionId, event) -> {
            boolean enterPressed = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_GO || enterPressed) {
                runCommand(binding.commandInput.getText().toString());
                return true;
            }
            return false;
        });
        binding.commandInput.setOnItemClickListener((parent, view, position, id) ->
                runCommand(String.valueOf(parent.getItemAtPosition(position))));
    }

    private void updateCommandSuggestions() {
        String wake = getString(R.string.command_verb_wake);
        String off = getString(R.string.command_verb_off);
        String edit = getString(R.string.command_verb_edit);

        List<String> suggestions = new ArrayList<>();
        for (Device device : devices) {
            String slug = ConsoleCommand.slug(device.name);
            suggestions.add(wake + " " + slug);
            if (ShutdownModelFactory.fromDevice(device).isPresent()) {
                suggestions.add(off + " " + slug);
            }
            suggestions.add(edit + " " + slug);
        }
        suggestions.add(getString(R.string.command_verb_add));

        binding.commandInput.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.console_suggestion_item, suggestions));
        binding.commandInput.setHint(devices.isEmpty()
                ? getString(R.string.command_verb_add)
                : wake + " " + ConsoleCommand.slug(devices.get(0).name));
    }

    private void runCommand(String input) {
        ConsoleCommand command = ConsoleCommand.parse(input, devices);
        switch (command.error) {
            case EMPTY:
                return;
            case UNKNOWN_VERB:
            case MISSING_DEVICE:
                showMessage(getString(R.string.command_unknown));
                return;
            case DEVICE_NOT_FOUND:
                showMessage(getString(R.string.command_device_not_found, command.query));
                return;
            default:
                break;
        }

        Context context = requireContext();
        Device device = command.device;
        switch (command.action) {
            case WAKE:
                AuthenticatedDeviceActionActivity.startWake(context, device.id);
                onDeviceWakeRequested(device.name);
                break;
            case SHUTDOWN:
                if (!ShutdownModelFactory.fromDevice(device).isPresent()) {
                    showMessage(getString(R.string.command_shutdown_unavailable, device.name));
                    return;
                }
                AuthenticatedDeviceActionActivity.startShutdown(context, device.id);
                break;
            case EDIT:
                AuthenticatedDeviceActionActivity.startEdit(context, device.id);
                break;
            case ADD:
                openAddDevice();
                break;
        }

        binding.commandInput.setText("");
        binding.commandInput.dismissDropDown();
        InputMethodManager imm = context.getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.commandInput.getWindowToken(), 0);
        }
        binding.commandInput.clearFocus();
    }

    private void openAddDevice() {
        NavHostFragment.findNavController(this).navigate(R.id.MainActivity_to_AddMachineActivity);
    }

    private void showMessage(String message) {
        if (binding != null) {
            Snackbar.make(binding.deviceListCoordinatorLayout, message, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.commandBar)
                    .show();
        }
    }

    private void instantiateRecyclerView() {
        List<Device> initialDataset = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(initialDataset, this::onDeviceWakeRequested, STATUS_TESTER_POOL);
        deviceListAdapter.setHasStableIds(true);
        RecyclerView devicesRecyclerView = binding.machineList;

        devicesRecyclerView.setAdapter(deviceListAdapter);
        devicesRecyclerView.setLayoutManager(getLayoutManager());
    }

    @NonNull
    private RecyclerView.LayoutManager getLayoutManager() {
        if (getResources().getBoolean(R.bool.isTablet)) {
            return new GridLayoutManagerWrapper(getContext(), 2);
        } else {
            return new LinearLayoutManagerWrapper(getContext());
        }
    }

    private void onDeviceWakeRequested(String deviceName) {
        Activity activity = getActivity();
        if (activity == null || binding == null) return;

        showMessage(activity.getString(R.string.wol_toast_sending_packet, deviceName));

        try {
            RatingHelper.INSTANCE.recordWakeAndMaybeRequestReview(activity);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
