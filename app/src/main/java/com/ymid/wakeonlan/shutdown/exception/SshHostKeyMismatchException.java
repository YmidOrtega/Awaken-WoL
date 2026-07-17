package com.ymid.wakeonlan.shutdown.exception;

public class SshHostKeyMismatchException extends Exception {

    private final String host;
    private final int port;
    private final String storedFingerprint;
    private final String presentedFingerprint;

    public SshHostKeyMismatchException(String host, int port, String storedFingerprint, String presentedFingerprint) {
        super("Host key for " + host + ":" + port + " changed. Pinned " + storedFingerprint
                + " but server presented " + presentedFingerprint);
        this.host = host;
        this.port = port;
        this.storedFingerprint = storedFingerprint;
        this.presentedFingerprint = presentedFingerprint;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getStoredFingerprint() {
        return storedFingerprint;
    }

    public String getPresentedFingerprint() {
        return presentedFingerprint;
    }
}
