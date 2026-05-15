package com.ymid.wakeonlan.persistence.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Device implements Parcelable {

    public int id;
    public String name;
    public String macAddress;
    public String broadcastAddress;
    public int port;
    public String statusIp;
    public String secureOnPassword;
    public boolean remoteShutdownEnabled;
    public String sshAddress;
    public Integer sshPort;
    public String sshUsername;
    public String sshPassword;
    public String sshCommand;
    public String shutdownOs;
    public String sshAuthType;
    public String sshKeyAlias;
    public String groupName;
    public String wanIp;
    public Integer wanPort;

    public Device(int id, String name, String macAddress, String broadcastAddress, int port, String statusIp,
                  String secureOnPassword, boolean remoteShutdownEnabled, String sshAddress, Integer sshPort,
                  String sshUsername, String sshPassword, String sshCommand, String shutdownOs,
                  String sshAuthType, String sshKeyAlias, String groupName, String wanIp, Integer wanPort) {
        this.id = id;
        this.name = name;
        this.macAddress = macAddress;
        this.broadcastAddress = broadcastAddress;
        this.port = port;
        this.statusIp = statusIp;
        this.secureOnPassword = secureOnPassword;
        this.remoteShutdownEnabled = remoteShutdownEnabled;
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

    public Device() {
    }

    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<>() {
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    private Device(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.macAddress = in.readString();
        this.broadcastAddress = in.readString();
        this.port = in.readInt();
        this.statusIp = in.readString();
        this.secureOnPassword = in.readString();
        this.remoteShutdownEnabled = in.readInt() >= 1;
        this.sshAddress = in.readString();
        this.sshPort = in.readInt();
        this.sshUsername = in.readString();
        this.sshPassword = in.readString();
        this.sshCommand = in.readString();
        this.shutdownOs = in.readString();
        this.sshAuthType = in.readString();
        this.sshKeyAlias = in.readString();
        this.groupName = in.readString();
        this.wanIp = in.readString();
        int wanPortVal = in.readInt();
        this.wanPort = wanPortVal == -1 ? null : wanPortVal;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(macAddress);
        dest.writeString(broadcastAddress);
        dest.writeInt(port);
        dest.writeString(statusIp);
        dest.writeString(secureOnPassword);
        dest.writeInt(remoteShutdownEnabled ? 1 : 0);
        dest.writeString(sshAddress);
        dest.writeInt(sshPort == null ? -1 : sshPort);
        dest.writeString(sshUsername);
        dest.writeString(sshPassword);
        dest.writeString(sshCommand);
        dest.writeString(shutdownOs == null ? "linux" : shutdownOs);
        dest.writeString(sshAuthType == null ? "password" : sshAuthType);
        dest.writeString(sshKeyAlias);
        dest.writeString(groupName);
        dest.writeString(wanIp);
        dest.writeInt(wanPort == null ? -1 : wanPort);
    }
}
