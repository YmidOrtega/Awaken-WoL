package com.ymid.wakeonlan.ui.list.status;

import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ymid.wakeonlan.persistence.models.DeviceStatus;

/**
 * In-memory record of the latest status checks, shared by the device list rows and the header.
 * Keeps a short latency history per device (for the sparkline) and remembers which devices were
 * just sent a magic packet, so a row can show "waking" until the device answers.
 * Only touched from the main thread.
 */
public final class DeviceStatusBoard {

    public static final int HISTORY_SIZE = 24;
    private static final long WAKE_WINDOW_MS = 120_000;

    private static final DeviceStatusBoard INSTANCE = new DeviceStatusBoard();

    private final Map<Integer, Deque<Long>> latencyHistory = new HashMap<>();
    private final Map<Integer, DeviceStatus> lastStatus = new HashMap<>();
    private final Map<Integer, Long> wakeSentAt = new HashMap<>();
    private final MutableLiveData<Long> changes = new MutableLiveData<>(0L);

    private DeviceStatusBoard() {
    }

    public static DeviceStatusBoard getInstance() {
        return INSTANCE;
    }

    /** Emits a new value every time a status is recorded. */
    public LiveData<Long> getChanges() {
        return changes;
    }

    @MainThread
    public void record(int deviceId, DeviceStatusSnapshot snapshot) {
        DeviceStatus status = snapshot.getStatus();
        if (status == DeviceStatus.ONLINE) {
            wakeSentAt.remove(deviceId);
            Long latency = snapshot.getLatencyMs();
            if (latency != null) {
                Deque<Long> history = latencyHistory.computeIfAbsent(deviceId, id -> new ArrayDeque<>());
                history.addLast(latency);
                while (history.size() > HISTORY_SIZE) {
                    history.removeFirst();
                }
            }
        } else if (status == DeviceStatus.OFFLINE) {
            latencyHistory.remove(deviceId);
        }

        DeviceStatus previous = lastStatus.put(deviceId, status);
        if (previous != status || status == DeviceStatus.ONLINE) {
            changes.setValue(SystemClock.elapsedRealtime());
        }
    }

    @MainThread
    public void markWakeSent(int deviceId) {
        wakeSentAt.put(deviceId, SystemClock.elapsedRealtime());
        changes.setValue(SystemClock.elapsedRealtime());
    }

    @MainThread
    public boolean isWaking(int deviceId) {
        Long sentAt = wakeSentAt.get(deviceId);
        if (sentAt == null) {
            return false;
        }
        if (SystemClock.elapsedRealtime() - sentAt > WAKE_WINDOW_MS) {
            wakeSentAt.remove(deviceId);
            return false;
        }
        return lastStatus.get(deviceId) != DeviceStatus.ONLINE;
    }

    @MainThread
    public List<Long> getLatencyHistory(int deviceId) {
        Deque<Long> history = latencyHistory.get(deviceId);
        return history == null ? new ArrayList<>() : new ArrayList<>(history);
    }

    @MainThread
    public int countOnline(Collection<Integer> deviceIds) {
        int online = 0;
        for (Integer id : deviceIds) {
            if (lastStatus.get(id) == DeviceStatus.ONLINE) {
                online++;
            }
        }
        return online;
    }
}
