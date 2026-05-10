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

            sshClient.authPassword(shutdownModel.getUsername(), shutdownModel.getPassword());
            shutdownExecutorListener.onLoginSuccessful();

            Session session = sshClient.startSession();
            shutdownExecutorListener.onSessionStartSuccessful();

            session.allocateDefaultPTY();

            if (blockDangerousCommands && isDangerousCommand(shutdownModel.getCommand(), os)) {
                // Prevent executing real shutdown commands during a test run.
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
        if (command == null) {
            return false;
        }
        String lower = command.toLowerCase();
        String osNorm = os == null ? "linux" : os.toLowerCase();

        if (osNorm.contains("linux")) {
            // quick contains checks
            if (lower.contains("poweroff") || lower.contains("halt") || lower.contains("init 0")) {
                return true;
            }
            // patterns for shutdown now or shutdown -h
            Pattern p = Pattern.compile("\\bshutdown\\b.*(now|\\-h|\\-P)", Pattern.CASE_INSENSITIVE);
            if (p.matcher(command).find()) {
                return true;
            }
            // systemctl poweroff/halt/reboot
            Pattern p2 = Pattern.compile("systemctl\\s+(poweroff|halt|reboot)", Pattern.CASE_INSENSITIVE);
            if (p2.matcher(command).find()) {
                return true;
            }
            return false;
        } else if (osNorm.contains("windows")) {
            // Windows shutdown commands: shutdown /s /t 0, shutdown -s -t 0, shutdown /p
            if (lower.contains("shutdown") && (lower.contains("/s") || lower.contains("-s") || lower.contains("/p") || lower.contains("-p"))) {
                // immediate shutdown if /t 0 or -t 0 present or /p/-p
                if (lower.contains("/t 0") || lower.contains("-t 0") || lower.contains("/p") || lower.contains("-p")) {
                    return true;
                }
            }
            if (lower.contains("poweroff") || lower.contains("shutdown.exe")) {
                return true;
            }
            return false;
        } else if (osNorm.contains("mac") || osNorm.contains("osx") || osNorm.contains("darwin")) {
            // macOS: shutdown -h now, sudo shutdown -h now, osascript shutdown commands
            if (lower.contains("shutdown") && (lower.contains("-h") || lower.contains("now"))) {
                return true;
            }
            if (lower.contains("osascript") && lower.contains("shut down")) {
                return true;
            }
            return false;
        }

        // default fallback: be conservative
        return isDangerousCommand(command, "linux");
    }

    private boolean sudoPrompt(ByteArrayOutputStream commandOutputStream) {
        return new String(commandOutputStream.toByteArray(), StandardCharsets.UTF_8).contains("[sudo] password for ");
    }
}
