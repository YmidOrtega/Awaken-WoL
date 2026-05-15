package com.ymid.wakeonlan.ui.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.databinding.FragmentListDevicesBinding;
import com.ymid.wakeonlan.persistence.entities.ActionType;
import com.ymid.wakeonlan.persistence.models.Device;
import com.ymid.wakeonlan.persistence.repository.ActionLogRepository;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.rating.RatingHelper;
import com.ymid.wakeonlan.ui.list.layoutmanager.GridLayoutManagerWrapper;
import com.ymid.wakeonlan.ui.list.layoutmanager.LinearLayoutManagerWrapper;
import com.ymid.wakeonlan.ui.list.status.pool.PingStatusTesterPool;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTestType;
import com.ymid.wakeonlan.ui.list.status.pool.StatusTesterPool;


public class DeviceListFragment extends Fragment {

    private DeviceRepository deviceRepository;
    private FragmentListDevicesBinding binding;
    private DeviceListAdapter deviceListAdapter;

    private static final StatusTesterPool STATUS_TESTER_POOL = PingStatusTesterPool.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentListDevicesBinding.inflate(inflater, container, false);
        binding.addDeviceFab.setOnClickListener(view -> Navigation.findNavController(container).navigate(R.id.MainActivity_to_AddMachineActivity));
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
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        deviceRepository = DeviceRepository.getInstance(getContext());

        instantiateRecyclerView();
        registerLiveDataObserver();
    }

    private void registerLiveDataObserver() {
        deviceRepository.getAllAsObservable()
                .observe(getViewLifecycleOwner(), devices -> deviceListAdapter.updateDataset(devices));
    }

    private void instantiateRecyclerView() {
        List<Device> initialDataset = DeviceRepository.getInstance(getContext()).getAll();
        deviceListAdapter = new DeviceListAdapter(initialDataset, buildDeviceClickedCallback(), STATUS_TESTER_POOL);
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

    @NonNull
    private DeviceClickedCallback buildDeviceClickedCallback() {
        return deviceName -> {
            android.app.Activity activity = getActivity();
            android.content.Context ctx = getContext();
            if (activity == null || ctx == null) return;

            String snackbarText = ctx.getString(R.string.wol_toast_sending_packet, deviceName);
            View coordinatorView = activity.findViewById(R.id.device_list_coordinator_layout);
            if (coordinatorView != null) {
                Snackbar.make(coordinatorView, snackbarText, Snackbar.LENGTH_SHORT).show();
            }

            ActionLogRepository.getInstance(ctx).log(deviceName, ActionType.WAKE);

            try {
                RatingHelper.INSTANCE.recordWakeAndMaybeRequestReview(activity);
            } catch (Exception ignored) {
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
