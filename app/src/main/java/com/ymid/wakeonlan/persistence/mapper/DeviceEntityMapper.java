package com.ymid.wakeonlan.persistence.mapper;

import com.ymid.wakeonlan.persistence.entities.DeviceEntity;
import com.ymid.wakeonlan.persistence.models.Device;

public class DeviceEntityMapper implements EntityMapper<Device, DeviceEntity> {

    @Override
    public Device entityToModel(DeviceEntity entity) {
        if (entity == null) {
            return new Device();
        }
        return new Device(entity.id, entity.name, entity.macAddress, entity.broadcastAddress, entity.port, entity.statusIp, entity.secureOnPassword,
                entity.enableRemoteShutdown, entity.sshAddress, entity.sshPort, entity.sshUsername, entity.sshPassword, entity.sshCommand, entity.shutdownOs == null ? "linux" : entity.shutdownOs);
    }

    @Override
    public DeviceEntity modelToEntity(Device model) {
        if (model == null) {
            return new DeviceEntity();
        }
        return new DeviceEntity(model.id, model.name, model.macAddress, model.broadcastAddress, model.port, model.statusIp, model.secureOnPassword,
                model.remoteShutdownEnabled, model.sshAddress, model.sshPort, model.sshUsername, model.sshPassword, model.sshCommand, model.shutdownOs == null ? "linux" : model.shutdownOs);
    }
}
