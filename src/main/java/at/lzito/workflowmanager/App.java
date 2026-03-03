package at.lzito.workflowmanager;

import com.formdev.flatlaf.FlatDarkLaf;
import at.lzito.workflowmanager.workflow.application.ActivateWorkflowUseCase;
import at.lzito.workflowmanager.workflow.application.ReloadWorkflowsUseCase;
import at.lzito.workflowmanager.workflow.application.SaveWorkflowsUseCase;
import at.lzito.workflowmanager.workflow.infrastructure.hotkey.JNativeHookRegistry;
import at.lzito.workflowmanager.workflow.infrastructure.persistence.JsonWorkflowRepository;
import at.lzito.workflowmanager.workflow.infrastructure.process.OsProcessLauncher;
import at.lzito.workflowmanager.workflow.presentation.MainWindow;

import javax.swing.*;
import java.nio.file.Files;
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

        // Infrastructure
        JsonWorkflowRepository repository     = new JsonWorkflowRepository(logger);
        OsProcessLauncher      launcher       = new OsProcessLauncher(logger);
        JNativeHookRegistry    hotkeyRegistry = new JNativeHookRegistry(logger);

        // Application use cases
        ActivateWorkflowUseCase activateUseCase = new ActivateWorkflowUseCase(repository, launcher, logger);
        ReloadWorkflowsUseCase  reloadUseCase   = new ReloadWorkflowsUseCase(repository, hotkeyRegistry, logger);
        SaveWorkflowsUseCase    saveUseCase     = new SaveWorkflowsUseCase(repository, logger);

        // Detect first run before the config file is created by reload
        boolean firstRun = !Files.exists(repository.configPath());

        // Register global hotkey hook
        hotkeyRegistry.register();

        // Ensure JNativeHook is cleaned up on any exit path
        Runtime.getRuntime().addShutdownHook(new Thread(hotkeyRegistry::unregister, "shutdown-hook"));

        // Build UI on the EDT, then point the logger at the window
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow(activateUseCase, reloadUseCase, saveUseCase, repository, firstRun);
            logRef.set(window::log);
        });
    }
}
