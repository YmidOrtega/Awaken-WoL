package com.ymid.wakeonlan.shutdown;

public class ShutdownModel {

    private final String sshAddress;
    private final int sshPort;
    private final String username;
    private final String password;
    private final String command;
    private final String sshAuthType;
    private final String sshKeyAlias;

    public ShutdownModel(String sshAddress, int sshPort, String username, String password, String command,
                         String sshAuthType, String sshKeyAlias) {
        this.sshAddress = sshAddress;
        this.sshPort = sshPort;
        this.username = username;
        this.password = password;
        this.command = command;
        this.sshAuthType = sshAuthType == null ? "password" : sshAuthType;
        this.sshKeyAlias = sshKeyAlias;
    }

    public ShutdownModel(String sshAddress, int sshPort, String username, String password, String command) {
        this(sshAddress, sshPort, username, password, command, "password", null);
    }

    public String getSshAddress() { return sshAddress; }
    public int getSshPort() { return sshPort; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getCommand() { return command; }
    public String getSshAuthType() { return sshAuthType; }
    public String getSshKeyAlias() { return sshKeyAlias; }

    public boolean isKeyAuth() {
        return "key".equalsIgnoreCase(sshAuthType);
    }
}
