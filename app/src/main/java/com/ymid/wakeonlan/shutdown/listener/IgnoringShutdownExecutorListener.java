package com.ymid.wakeonlan.shutdown.listener;

import com.ymid.wakeonlan.shutdown.ShutdownModel;

public class IgnoringShutdownExecutorListener implements ShutdownExecutorListener {

    @Override
    public void onTargetHostReached() {
        // Ignore
    }

    @Override
    public void onLoginSuccessful() {
        // Ignore
    }

    @Override
    public void onSessionStartSuccessful() {
        // Ignore
    }

    @Override
    public void onCommandExecuteSuccessful() {
        // Ignore
    }

    @Override
    public void onSudoPromptTriggered(ShutdownModel shutdownModel) {
        // Ignore
    }

    @Override
    public void onDangerousCommandDetected(ShutdownModel shutdownModel) {
        // Ignore
    }

    @Override
    public void onGeneralError(Exception exception, ShutdownModel shutdownModel) {
        // Ignore
    }
}
