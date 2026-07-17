package com.ymid.wakeonlan.persistence.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ymid.wakeonlan.persistence.DatabaseInstanceManager;
import com.ymid.wakeonlan.persistence.DeviceDao;
import com.ymid.wakeonlan.persistence.entities.DeviceEntity;
import com.ymid.wakeonlan.persistence.mapper.DeviceEntityMapper;
import com.ymid.wakeonlan.persistence.models.Device;

public class DeviceRepository {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final DeviceEntityMapper deviceEntityMapper = new DeviceEntityMapper();
    private final Context appContext;
    private volatile DeviceDao deviceDao;

    DeviceRepository(Context appContext) {
        this.appContext = appContext;
    }

    public static DeviceRepository getInstance(Context context) {
        return new DeviceRepository(context.getApplicationContext());
    }

    private DeviceDao dao() {
        if (deviceDao == null) {
            synchronized (this) {
                if (deviceDao == null) {
                    deviceDao = DatabaseInstanceManager.getInstance(appContext).deviceDao();
                }
            }
        }
        return deviceDao;
    }

    public List<Device> getAll() {
        return dao().getAll().stream()
                .map(deviceEntityMapper::entityToModel)
                .collect(Collectors.toList());
    }

    public void getAllAsync(Consumer<List<Device>> callback) {
        EXECUTOR.execute(() -> {
            List<Device> devices = getAll();
            MAIN_HANDLER.post(() -> callback.accept(devices));
        });
    }

    public LiveData<List<Device>> getAllAsObservable() {
        return Transformations.map(dao().getAllAsObservable(), input ->
                input.stream().map(deviceEntityMapper::entityToModel).collect(Collectors.toList()));
    }

    public Device getById(int id) {
        DeviceEntity entity = dao().getById(id);
        return entity == null ? null : deviceEntityMapper.entityToModel(entity);
    }

    public void getByIdAsync(int id, Consumer<Device> callback) {
        EXECUTOR.execute(() -> {
            Device device = getById(id);
            MAIN_HANDLER.post(() -> callback.accept(device));
        });
    }

    public Device getByName(String name) {
        DeviceEntity entity = dao().getByName(name);
        return entity == null ? null : deviceEntityMapper.entityToModel(entity);
    }

    public void getByNameAsync(String name, Consumer<Device> callback) {
        EXECUTOR.execute(() -> {
            Device device = getByName(name);
            MAIN_HANDLER.post(() -> callback.accept(device));
        });
    }

    public void insertAll(Device... devices) {
        Arrays.stream(devices)
                .map(deviceEntityMapper::modelToEntity)
                .forEach(dao()::insertAll);
    }

    public void insertAllAsync(Device... devices) {
        EXECUTOR.execute(() -> insertAll(devices));
    }

    public void update(Device device) {
        dao().update(deviceEntityMapper.modelToEntity(device));
    }

    public void updateAsync(Device device) {
        EXECUTOR.execute(() -> update(device));
    }

    public void delete(Device device) {
        dao().delete(deviceEntityMapper.modelToEntity(device));
    }

    public void deleteAsync(Device device) {
        EXECUTOR.execute(() -> delete(device));
    }

    public void replaceAllDevices(Device... devices) {
        DeviceEntity[] deviceEntities = Arrays.stream(devices)
                .map(deviceEntityMapper::modelToEntity)
                .toArray(DeviceEntity[]::new);

        dao().replaceAllDevices(deviceEntities);
    }

}
