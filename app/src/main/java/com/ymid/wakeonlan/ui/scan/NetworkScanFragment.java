package com.ymid.wakeonlan.ui.scan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.concurrent.atomic.AtomicInteger;

import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.databinding.FragmentNetworkScanBinding;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.ui.ConsoleHeaderHost;
import com.ymid.wakeonlan.ui.list.LocalSubnet;
import com.ymid.wakeonlan.ui.list.layoutmanager.LinearLayoutManagerWrapper;
import com.ymid.wakeonlan.ui.scan.callbacks.ScanCallback;
import com.ymid.wakeonlan.ui.scan.model.NetworkScanDevice;

public class NetworkScanFragment extends Fragment {

    private FragmentNetworkScanBinding binding;
    private NetworkScanAdapter networkScanAdapter;
    private final AtomicInteger scannedAddresses = new AtomicInteger();
    private boolean scanning;
    // Callbacks from an older scan (e.g. one started before the view was recreated) are ignored
    private volatile int scanGeneration;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNetworkScanBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupRescanControls();
        DeviceRepository.getInstance(getContext()).getAllAsObservable()
                .observe(getViewLifecycleOwner(), devices -> networkScanAdapter.setKnownDevices(devices));

        startNetworkScan();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHeader();
    }

    private void startNetworkScan() {
        if (scanning) {
            return;
        }
        scanning = true;
        scannedAddresses.set(0);
        networkScanAdapter.clearDataset();
        networkScanAdapter.setScanning(true);
        binding.scanRescan.setVisibility(View.GONE);
        updateProgress();
        updateHeader();

        new NetworkScanTask(getScanCallback(++scanGeneration)).startScan(getContext());
    }

    private void setupRescanControls() {
        binding.scanRescan.setOnClickListener(v -> startNetworkScan());
        // The block bar already shows progress; pulling down only restarts the scan
        binding.swipeRefresh.setColorSchemeResources(R.color.ledOnlineColor);
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surfaceVariantColor);
        binding.swipeRefresh.setOnRefreshListener(() -> {
            binding.swipeRefresh.setRefreshing(false);
            startNetworkScan();
        });
    }

    private void updateProgress() {
        if (binding == null) {
            return;
        }
        int scanned = scannedAddresses.get();
        binding.scanProgressCount.setText(String.valueOf(scanned));
        binding.scanProgressTotal.setText(getString(R.string.scan_progress_total, NetworkScanTask.ADDRESS_COUNT));
        binding.scanProgressBar.setProgress(scanned / (float) NetworkScanTask.ADDRESS_COUNT);
    }

    private void updateHeader() {
        if (!(getActivity() instanceof ConsoleHeaderHost) || !isResumed()) {
            return;
        }
        ConsoleHeaderHost host = (ConsoleHeaderHost) getActivity();
        String subnet = LocalSubnet.describe(requireContext());
        String network = subnet == null ? getString(R.string.header_no_network) : subnet;
        host.setHeaderSubtitle(getString(scanning ? R.string.scan_subtitle_running : R.string.scan_subtitle_done, network));
        host.setHeaderCounter(String.valueOf(networkScanAdapter.getFoundCount()), getString(R.string.scan_found_caption));
    }

    private ScanCallback getScanCallback(int generation) {

        return new ScanCallback() {
            @Override
            public void onError(int errorStringReference) {
                runOnUiThread(() ->
                        Toast.makeText(getContext(), errorStringReference, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDeviceFound(String ip, String hostName) {
                NetworkScanDevice networkScanDevice = new NetworkScanDevice();
                networkScanDevice.setIpAddress(ip);
                if (hostName != null && !ip.equals(hostName) && !hostName.isEmpty()) {
                    networkScanDevice.setName(hostName);
                }

                runOnUiThread(() -> {
                    networkScanAdapter.addDevice(networkScanDevice);
                    updateHeader();
                });
            }

            @Override
            public void onAddressScanned() {
                if (generation != scanGeneration) {
                    return;
                }
                scannedAddresses.incrementAndGet();
                runOnUiThread(NetworkScanFragment.this::updateProgress);
            }

            private void runOnUiThread(Runnable runnable) {
                FragmentActivity activity = getActivity();
                if (activity == null) {
                    // Activity has already finished, Threads were still running. Do nothing
                    return;
                }
                activity.runOnUiThread(() -> {
                    if (binding != null && generation == scanGeneration) {
                        runnable.run();
                    }
                });
            }

            @Override
            public void onTaskEnd() {
                runOnUiThread(() -> {
                    scanning = false;
                    // Addresses that timed out never report back; the scan is still complete
                    scannedAddresses.set(NetworkScanTask.ADDRESS_COUNT);
                    updateProgress();
                    networkScanAdapter.setScanning(false);
                    binding.scanRescan.setVisibility(View.VISIBLE);
                    updateHeader();
                });
            }
        };
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.networkList;

        networkScanAdapter = new NetworkScanAdapter();
        recyclerView.setAdapter(networkScanAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManagerWrapper(getContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        scanning = false;
    }
}
