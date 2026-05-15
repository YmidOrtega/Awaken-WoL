package com.ymid.wakeonlan.shutdown;

import android.util.Log;

import com.google.common.base.Throwables;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.LoggerFactory;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.ymid.wakeonlan.security.AndroidKeyStoreKeyProvider;
import com.ymid.wakeonlan.shutdown.exception.CommandExecuteException;
import com.ymid.wakeonlan.shutdown.listener.ShutdownExecutorListener;

public class ShutdownRunnable implements Runnable {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int EXECUTE_TIMEOUT = 500;

    private final ShutdownModel shutdownModel;
    private final ShutdownExecutorListener shutdownExecutorListener;
    private final String os;
    private final boolean blockDangerousCommands;

    public ShutdownRunnable(ShutdownModel shutdownModel, ShutdownExecutorListener shutdownExecutorListener) {
        this(shutdownModel, shutdownExecutorListener, "linux", false);
    }

    public ShutdownRunnable(ShutdownModel shutdownModel, ShutdownExecutorListener shutdownExecutorListener, String os) {
        this(shutdownModel, shutdownExecutorListener, os, false);
    }

    public ShutdownRunnable(ShutdownModel shutdownModel, ShutdownExecutorListener shutdownExecutorListener, String os, boolean blockDangerousCommands) {
        this.shutdownModel = shutdownModel;
        this.shutdownExecutorListener = shutdownExecutorListener;
        this.os = os == null ? "linux" : os.toLowerCase();
        this.blockDangerousCommands = blockDangerousCommands;
    }

    @Override
    public void run() {
        ByteArrayOutputStream commandOutputStream = new ByteArrayOutputStream();

        try (SSHClient sshClient = new SSHClient()) {
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.setConnectTimeout(CONNECT_TIMEOUT);
            sshClient.connect(shutdownModel.getSshAddress(), shutdownModel.getSshPort());
            shutdownExecutorListener.onTargetHostReached();

            if (shutdownModel.isKeyAuth()) {
                sshClient.authPublickey(shutdownModel.getUsername(),
                        new AndroidKeyStoreKeyProvider(shutdownModel.getSshKeyAlias()));
            } else {
                sshClient.authPassword(shutdownModel.getUsername(), shutdownModel.getPassword());
            }
            shutdownExecutorListener.onLoginSuccessful();

            Session session = sshClient.startSession();
            shutdownExecutorListener.onSessionStartSuccessful();

            session.allocateDefaultPTY();

            if (blockDangerousCommands && isDangerousCommand(shutdownModel.getCommand(), os)) {
                shutdownExecutorListener.onDangerousCommandDetected(shutdownModel);
                return;
            }

            Session.Command exec = session.exec(shutdownModel.getCommand());
            new StreamCopier(exec.getInputStream(), commandOutputStream, LoggerFactory.DEFAULT)
                    .bufSize(exec.getLocalMaxPacketSize())
                    .spawn("stdout");

            exec.join(EXECUTE_TIMEOUT, TimeUnit.MILLISECONDS);
            Integer exitStatus = exec.getExitStatus();
            if (exitStatus != 0) {
                throw new CommandExecuteException("Command exited with status code " + exitStatus, exitStatus);
            }

            shutdownExecutorListener.onCommandExecuteSuccessful();
        } catch (Exception e) {
            if (Throwables.getRootCause(e) instanceof TransportException) {
                shutdownExecutorListener.onCommandExecuteSuccessful();
                return;
            }

            Log.e(ShutdownRunnable.class.getSimpleName(), "Error during SSH execution", e);

            if (sudoPrompt(commandOutputStream)) {
                shutdownExecutorListener.onSudoPromptTriggered(shutdownModel);
                return;
            }

            shutdownExecutorListener.onGeneralError(e, shutdownModel);
        }
    }

    private boolean isDangerousCommand(String command, String os) {
        if (command == null) return false;
        String lower = command.toLowerCase();
        String osNorm = os == null ? "linux" : os.toLowerCase();

        if (osNorm.contains("linux")) {
            if (lower.contains("poweroff") || lower.contains("halt") || lower.contains("init 0")) return true;
            if (Pattern.compile("\\bshutdown\\b.*(now|\\-h|\\-P)", Pattern.CASE_INSENSITIVE).matcher(command).find()) return true;
            if (Pattern.compile("systemctl\\s+(poweroff|halt|reboot)", Pattern.CASE_INSENSITIVE).matcher(command).find()) return true;
            return false;
        } else if (osNorm.contains("windows")) {
            if (lower.contains("shutdown") && (lower.contains("/s") || lower.contains("-s") || lower.contains("/p") || lower.contains("-p"))) {
                if (lower.contains("/t 0") || lower.contains("-t 0") || lower.contains("/p") || lower.contains("-p")) return true;
            }
            return lower.contains("poweroff") || lower.contains("shutdown.exe");
        } else if (osNorm.contains("mac") || osNorm.contains("osx") || osNorm.contains("darwin")) {
            if (lower.contains("shutdown") && (lower.contains("-h") || lower.contains("now"))) return true;
            return lower.contains("osascript") && lower.contains("shut down");
        }

        return isDangerousCommand(command, "linux");
    }

    private boolean sudoPrompt(ByteArrayOutputStream commandOutputStream) {
        return new String(commandOutputStream.toByteArray(), StandardCharsets.UTF_8).contains("[sudo] password for ");
    }
}
