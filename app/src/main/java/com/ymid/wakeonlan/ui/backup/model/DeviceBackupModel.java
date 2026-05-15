package com.ymid.wakeonlan.ui.backup.model;

import com.google.gson.annotations.SerializedName;

import com.ymid.wakeonlan.persistence.models.Device;

public class DeviceBackupModel {

    /*
    Aliases for each field have to be kept for some time because backups exported prior to
    version 1.6.5 were created using obfuscated classes. This means that each field was written
    to JSON with its obfuscated name ("a", "b", and so on).
     */

    @SerializedName(value = "id", alternate = "a")
    public int id;

    @SerializedName(value = "name", alternate = "b")
    public String name;

    @SerializedName(value = "mac_address", alternate = "c")
    public String macAddress;

    @SerializedName(value = "broadcast_address", alternate = "d")
    public String broadcastAddress;

    @SerializedName(value = "port", alternate = "e")
    public int port;

    @SerializedName(value = "status_ip", alternate = "f")
    public String statusIp;

    @SerializedName(value = "secure_on_password", alternate = "g")
    public String secureOnPassword;

    @SerializedName(value = "remote_shutdown_enabled")
    public boolean remoteShutdownEnabled;

    @SerializedName(value = "ssh_address")
    public String sshAddress;

    @SerializedName(value = "ssh_port")
    public Integer sshPort;

    @SerializedName(value = "ssh_username")
    public String sshUsername;

    @SerializedName(value = "ssh_password")
    public String sshPassword;

    @SerializedName(value = "ssh_command")
    public String sshCommand;

    @SerializedName(value = "shutdown_os")
    public String shutdownOs;

    @SerializedName(value = "group_name")
    public String groupName;

    @SerializedName(value = "wan_ip")
    public String wanIp;

    @SerializedName(value = "wan_port")
    public Integer wanPort;

    public DeviceBackupModel(Device device) {
        this.id = device.id;
        this.name = device.name;
        this.macAddress = device.macAddress;
        this.broadcastAddress = device.broadcastAddress;
        this.port = device.port;
        this.statusIp = device.statusIp;
        this.secureOnPassword = device.secureOnPassword;
        this.remoteShutdownEnabled = device.remoteShutdownEnabled;
        this.sshAddress = device.sshAddress;
        this.sshPort = device.sshPort;
        this.sshUsername = device.sshUsername;
        this.sshPassword = device.sshPassword;
        this.sshCommand = device.sshCommand;
        this.shutdownOs = device.shutdownOs;
        this.groupName = device.groupName;
        this.wanIp = device.wanIp;
        this.wanPort = device.wanPort;
    }

    public Device toModel() {
        return new Device(
                this.id, this.name, this.macAddress, this.broadcastAddress, this.port,
                this.statusIp, this.secureOnPassword, this.remoteShutdownEnabled,
                this.sshAddress, this.sshPort, this.sshUsername, this.sshPassword,
                this.sshCommand, this.shutdownOs == null ? "linux" : this.shutdownOs,
                "password", null, this.groupName, this.wanIp, this.wanPort);
    }

    @SuppressWarnings("unused")
    public DeviceBackupModel() {
    }
}
