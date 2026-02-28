package at.lzito.workflowmanager.infrastructure.process;

import at.lzito.workflowmanager.application.port.ProcessLauncherPort;
import at.lzito.workflowmanager.domain.AppEntry;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ProcessLauncherPort implementation that spawns real OS processes and opens URLs.
 */
public class OsProcessLauncher implements ProcessLauncherPort {

    private final Consumer<String> logger;

    public OsProcessLauncher(Consumer<String> logger) {
        this.logger = logger;
    }

    @Override
    public void launch(AppEntry entry) {
        if (entry.isUrl()) {
            openUrl(entry.getUrl());
        } else {
            launchProcess(entry);
        }
    }

    @Override
    public void kill(String processName) {
        try {
            ProcessBuilder pb;
            if (isWindows()) {
                pb = new ProcessBuilder("taskkill", "/F", "/IM", processName);
            } else {
                pb = new ProcessBuilder("pkill", "-f", processName);
            }
            pb.start();
            logger.accept("Killed: " + processName);
        } catch (IOException e) {
            logger.accept("Failed to kill " + processName + ": " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void launchProcess(AppEntry entry) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(entry.getPath());
            cmd.addAll(entry.getArgs());
            new ProcessBuilder(cmd).start();
            logger.accept("Started: " + entry.getName());
        } catch (IOException e) {
            logger.accept("Failed to start " + entry.getName() + ": " + e.getMessage());
        }
    }

    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
            logger.accept("Opened URL: " + url);
        } catch (IOException e) {
            logger.accept("Failed to open URL " + url + ": " + e.getMessage());
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
