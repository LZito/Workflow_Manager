package at.lzito.workflowmanager.application;

import at.lzito.workflowmanager.application.port.HotkeyRegistryPort;
import at.lzito.workflowmanager.domain.Workflow;
import at.lzito.workflowmanager.domain.WorkflowRepository;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reloads workflows from the config file and rebinds all hotkeys.
 */
public class ReloadWorkflowsUseCase {

    private final WorkflowRepository repository;
    private final HotkeyRegistryPort hotkeyRegistry;
    private final Consumer<String>   logger;

    public ReloadWorkflowsUseCase(
            WorkflowRepository repository,
            HotkeyRegistryPort hotkeyRegistry,
            Consumer<String>   logger) {
        this.repository     = repository;
        this.hotkeyRegistry = hotkeyRegistry;
        this.logger         = logger;
    }

    /**
     * Reload the config and rebind hotkeys.
     *
     * @param onHotkeyActivate callback to invoke (on the JNativeHook thread) when a hotkey fires
     * @return the newly loaded list of workflows
     * @throws IOException if the config cannot be read
     */
    public List<Workflow> execute(Consumer<Workflow> onHotkeyActivate) throws IOException {
        repository.reload();
        List<Workflow> workflows = repository.findAll();
        hotkeyRegistry.bindWorkflows(workflows, onHotkeyActivate);
        logger.accept("Config loaded from: " + repository.configPath());
        return workflows;
    }
}
