package at.lzito.workflowmanager.engine;

import at.lzito.workflowmanager.model.Workflow;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WorkflowEngine {

    private final Consumer<String> logger;

    public WorkflowEngine(Consumer<String> logger) {
        this.logger = logger;
    }

    public void activate(Workflow workflow, List<Workflow> allWorkflows) {
        if (workflow.isCloseOthers()) {
            allWorkflows.stream()
                    .filter(w -> w != workflow)
                    .forEach(this::closeWorkflow);
        } else if (workflow.getClose() != null) {
            workflow.getClose().forEach(this::killProcess);
        }

        if (workflow.getOpen() != null) {
            for (Workflow.AppEntry entry : workflow.getOpen()) {
                if (entry.getDelayMs() > 0) {
                    try { Thread.sleep(entry.getDelayMs()); } catch (InterruptedException ignored) {}
                }
                if (entry.getUrl() != null && !entry.getUrl().isBlank()) {
                    openUrl(entry.getUrl());
                } else {
                    launch(entry);
                }
            }
        }
    }

    private void closeWorkflow(Workflow workflow) {
        if (workflow.getClose() != null) {
            workflow.getClose().forEach(this::killProcess);
        }
    }

    private void killProcess(String processName) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
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

    private void launch(Workflow.AppEntry entry) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(entry.getPath());
            if (entry.getArgs() != null) cmd.addAll(entry.getArgs());
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
}
