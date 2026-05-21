package com.ymid.wakeonlan.persistence.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.ymid.wakeonlan.persistence.DatabaseInstanceManager;
import com.ymid.wakeonlan.persistence.DeviceDao;
import com.ymid.wakeonlan.persistence.entities.DeviceEntity;
import com.ymid.wakeonlan.persistence.mapper.DeviceEntityMapper;
import com.ymid.wakeonlan.persistence.models.Device;

public class DeviceRepository {

    private final DeviceEntityMapper deviceEntityMapper = new DeviceEntityMapper();
    private final DeviceDao deviceDao;

    DeviceRepository(DeviceDao deviceDao) {
        this.deviceDao = deviceDao;
    }

    public static DeviceRepository getInstance(Context context) {
        return new DeviceRepository(DatabaseInstanceManager.getInstance(context).deviceDao());
    }


    public List<Device> getAll() {
        return deviceDao.getAll().stream()
                .map(deviceEntityMapper::entityToModel)
                .collect(Collectors.toList());
    }

    public LiveData<List<Device>> getAllAsObservable() {
        return Transformations.map(deviceDao.getAllAsObservable(), input ->
                input.stream().map(deviceEntityMapper::entityToModel).collect(Collectors.toList()));
    }

    public Device getById(int id) {
        return deviceEntityMapper.entityToModel(deviceDao.getById(id));
    }

    public Device getByName(String name) {
        return deviceEntityMapper.entityToModel(deviceDao.getByName(name));
    }

    public void insertAll(Device... devices) {
        Arrays.stream(devices)
                .map(deviceEntityMapper::modelToEntity)
                .forEach(deviceDao::insertAll);
    }

    public void update(Device device) {
        deviceDao.update(deviceEntityMapper.modelToEntity(device));
    }

    public void delete(Device device) {
        deviceDao.delete(deviceEntityMapper.modelToEntity(device));
    }

    public void replaceAllDevices(Device... devices) {
        DeviceEntity[] deviceEntities = Arrays.stream(devices)
                .map(deviceEntityMapper::modelToEntity)
                .toArray(DeviceEntity[]::new);

        deviceDao.replaceAllDevices(deviceEntities);
    }

}
