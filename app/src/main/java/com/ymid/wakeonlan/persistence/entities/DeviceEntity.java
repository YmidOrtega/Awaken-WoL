package com.ymid.wakeonlan.persistence.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "Devices")
public class DeviceEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "mac_address")
    public String macAddress;

    @ColumnInfo(name = "broadcast_address")
    public String broadcastAddress;

    @ColumnInfo(name = "port")
    public int port;

    @ColumnInfo(name = "status_ip")
    public String statusIp;

    @ColumnInfo(name = "secure_on_password")
    public String secureOnPassword;

    @ColumnInfo(name = "enable_remote_shutdown", defaultValue = "0")
    public boolean enableRemoteShutdown;

    @ColumnInfo(name = "ssh_address")
    public String sshAddress;

    @ColumnInfo(name = "ssh_port")
    public Integer sshPort;

    @ColumnInfo(name = "ssh_user")
    public String sshUsername;

    @ColumnInfo(name = "ssh_password")
    public String sshPassword;

    @ColumnInfo(name = "ssh_command")
    public String sshCommand;

    @ColumnInfo(name = "shutdown_os", defaultValue = "'linux'")
    public String shutdownOs;

    @ColumnInfo(name = "ssh_auth_type", defaultValue = "'password'")
    public String sshAuthType;

    @ColumnInfo(name = "ssh_key_alias")
    public String sshKeyAlias;

    @ColumnInfo(name = "group_name")
    public String groupName;

    @ColumnInfo(name = "wan_ip")
    public String wanIp;

    @ColumnInfo(name = "wan_port")
    public Integer wanPort;

    @Ignore
    public DeviceEntity(int id, String name, String macAddress, String broadcastAddress, int port, String statusIp,
                        String secureOnPassword, boolean enableRemoteShutdown, String sshAddress, Integer sshPort,
                        String sshUsername, String sshPassword, String sshCommand, String shutdownOs,
                        String sshAuthType, String sshKeyAlias, String groupName, String wanIp, Integer wanPort) {
        this.id = id;
        this.name = name;
        this.macAddress = macAddress;
        this.broadcastAddress = broadcastAddress;
        this.port = port;
        this.statusIp = statusIp;
        this.secureOnPassword = secureOnPassword;
        this.enableRemoteShutdown = enableRemoteShutdown;
        this.sshAddress = sshAddress;
        this.sshPort = sshPort;
        this.sshUsername = sshUsername;
        this.sshPassword = sshPassword;
        this.sshCommand = sshCommand;
        this.shutdownOs = shutdownOs;
        this.sshAuthType = sshAuthType;
        this.sshKeyAlias = sshKeyAlias;
        this.groupName = groupName;
        this.wanIp = wanIp;
        this.wanPort = wanPort;
    }

    public DeviceEntity() {
    }
}
