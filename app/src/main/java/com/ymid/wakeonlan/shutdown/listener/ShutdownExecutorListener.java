package com.ymid.wakeonlan.shutdown.listener;

import androidx.annotation.Nullable;

import com.ymid.wakeonlan.shutdown.ShutdownModel;

public interface ShutdownExecutorListener {

    void onTargetHostReached();

    void onLoginSuccessful();

    void onSessionStartSuccessful();

    void onCommandExecuteSuccessful();

    void onSudoPromptTriggered(ShutdownModel shutdownModel);

    void onDangerousCommandDetected(ShutdownModel shutdownModel);

    void onGeneralError(Exception exception, @Nullable ShutdownModel shutdownModel);

}
