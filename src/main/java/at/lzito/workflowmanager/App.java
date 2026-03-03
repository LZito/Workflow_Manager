package at.lzito.workflowmanager;

import com.formdev.flatlaf.FlatDarkLaf;
import at.lzito.workflowmanager.updater.application.CheckForUpdateUseCase;
import at.lzito.workflowmanager.updater.infrastructure.GitHubReleaseRepository;
import at.lzito.workflowmanager.updater.infrastructure.JarSelfUpdater;
import at.lzito.workflowmanager.workflow.application.WorkflowAppService;
import at.lzito.workflowmanager.workflow.infrastructure.hotkey.JNativeHookRegistry;
import at.lzito.workflowmanager.workflow.infrastructure.persistence.JsonWorkflowRepository;
import at.lzito.workflowmanager.workflow.infrastructure.process.OsProcessLauncher;
import at.lzito.workflowmanager.workflow.presentation.MainWindow;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Composition root — wires up the dependency graph and starts the application.
 * No business logic lives here.
 */
public class App {

    public static void main(String[] args) {
        FlatDarkLaf.setup();

        // Shared logger: starts as stdout, gets rewired to the window once it exists
        AtomicReference<Consumer<String>> logRef = new AtomicReference<>(System.out::println);
        Consumer<String> logger = msg -> logRef.get().accept(msg);

        // ── Workflow bounded context ───────────────────────────────────────────
        JsonWorkflowRepository repository     = new JsonWorkflowRepository(logger);
        OsProcessLauncher      launcher       = new OsProcessLauncher(logger);
        JNativeHookRegistry    hotkeyRegistry = new JNativeHookRegistry(logger);

        WorkflowAppService workflowAppService = new WorkflowAppService(repository, launcher, hotkeyRegistry, logger);

        // Detect first run before the config file is created by reload
        boolean firstRun = !Files.exists(repository.configPath());

        // Register global hotkey hook
        hotkeyRegistry.register();

        // Ensure JNativeHook is cleaned up on any exit path
        Runtime.getRuntime().addShutdownHook(new Thread(hotkeyRegistry::unregister, "shutdown-hook"));

        // ── Updater bounded context ────────────────────────────────────────────
        String   currentVersion = readVersion();
        String[] githubConfig   = readGithubConfig();
        GitHubReleaseRepository releaseRepo     = new GitHubReleaseRepository(githubConfig[0], githubConfig[1], currentVersion);
        CheckForUpdateUseCase   checkForUpdate  = new CheckForUpdateUseCase(releaseRepo, currentVersion);

        // ── UI ────────────────────────────────────────────────────────────────
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow(workflowAppService, firstRun);
            logRef.set(window::log);

            // Background update check — runs after a short delay so startup feels instant.
            // All failures are silently swallowed; a broken network must never affect the app.
            Thread updateThread = new Thread(() -> {
                try {
                    Thread.sleep(4_000);
                    checkForUpdate.execute().ifPresent(release ->
                            SwingUtilities.invokeLater(() ->
                                    window.promptUpdate(release.getVersion(), () -> {
                                        try {
                                            JarSelfUpdater.apply(release.getDownloadUrl());
                                        } catch (Exception ex) {
                                            logger.accept("Update failed: " + ex.getMessage());
                                        }
                                    })));
                } catch (Exception ignored) {}
            }, "update-check");
            updateThread.setDaemon(true);
            updateThread.start();
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Reads the running version from the JAR manifest, or falls back to {@code "0.0.0"}. */
    private static String readVersion() {
        String v = App.class.getPackage().getImplementationVersion();
        return v != null ? v : "0.0.0";
    }

    /** Reads {@code github.owner} and {@code github.repo} from {@code app.properties}. */
    private static String[] readGithubConfig() {
        Properties props = new Properties();
        try (InputStream is = App.class.getResourceAsStream("/app.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}
        return new String[]{
            props.getProperty("github.owner", "").trim(),
            props.getProperty("github.repo", "WorkflowManager").trim()
        };
    }
}
