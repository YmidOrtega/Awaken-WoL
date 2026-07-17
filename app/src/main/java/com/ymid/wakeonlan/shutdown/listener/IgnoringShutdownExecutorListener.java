package com.ymid.wakeonlan.shutdown.listener;

import com.ymid.wakeonlan.shutdown.ShutdownModel;

public class IgnoringShutdownExecutorListener implements ShutdownExecutorListener {

    @Override
    public void onTargetHostReached() {
    }

    @Override
    public void onLoginSuccessful() {
    }

    @Override
    public void onSessionStartSuccessful() {
    }

    @Override
    public void onCommandExecuteSuccessful() {
    }

    @Override
    public void onSudoPromptTriggered(ShutdownModel shutdownModel) {
    }

    @Override
    public void onDangerousCommandDetected(ShutdownModel shutdownModel) {
    }

    @Override
    public void onGeneralError(Exception exception, ShutdownModel shutdownModel) {
    }
}
