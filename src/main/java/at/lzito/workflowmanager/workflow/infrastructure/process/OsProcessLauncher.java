package at.lzito.workflowmanager.workflow.infrastructure.process;

import at.lzito.workflowmanager.workflow.application.ProcessLauncher;
import at.lzito.workflowmanager.workflow.domain.AppEntry;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ProcessLauncher implementation that spawns real OS processes and opens URLs.
 */
public class OsProcessLauncher implements ProcessLauncher {

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
            ProcessBuilder pb = isWindows()
                    ? new ProcessBuilder("taskkill", "/F", "/IM", processName)
                    : new ProcessBuilder("pkill", "-f", processName);
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
