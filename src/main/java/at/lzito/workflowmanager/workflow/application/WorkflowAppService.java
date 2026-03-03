package at.lzito.workflowmanager.workflow.application;

import at.lzito.workflowmanager.workflow.domain.AppEntry;
import at.lzito.workflowmanager.workflow.domain.Workflow;
import at.lzito.workflowmanager.workflow.domain.WorkflowRepository;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Application service for the workflow bounded context.
 *
 * <p>Thin coordinator — all business logic lives in the domain.
 * The presentation layer interacts exclusively through this class.
 */
public class WorkflowAppService {

    private final WorkflowRepository repository;
    private final ProcessLauncher    launcher;
    private final HotkeyRegistry     hotkeyRegistry;
    private final Consumer<String>   logger;

    public WorkflowAppService(
            WorkflowRepository repository,
            ProcessLauncher    launcher,
            HotkeyRegistry     hotkeyRegistry,
            Consumer<String>   logger) {
        this.repository     = repository;
        this.launcher       = launcher;
        this.hotkeyRegistry = hotkeyRegistry;
        this.logger         = logger;
    }

    /** Returns the currently loaded list of workflows. */
    public List<Workflow> getAll() {
        return repository.findAll();
    }

    /**
     * Reloads workflows from the config file and rebinds all hotkeys.
     *
     * @param onHotkeyActivate callback invoked (on the JNativeHook thread) when a hotkey fires
     * @return the newly loaded list of workflows
     * @throws IOException if the config cannot be read
     */
    public List<Workflow> reload(Consumer<Workflow> onHotkeyActivate) throws IOException {
        repository.reload();
        List<Workflow> workflows = repository.findAll();
        hotkeyRegistry.bindWorkflows(workflows, onHotkeyActivate);
        logger.accept("Config loaded from: " + repository.configPath());
        return workflows;
    }

    /**
     * Activates a workflow: optionally closes other processes, then launches each app/URL.
     *
     * <p>Must be called on a background thread — never on the EDT.
     */
    public void activate(Workflow workflow) {
        logger.accept("Activating: " + workflow.displayName());

        if (workflow.isCloseOthers()) {
            repository.findAll().stream()
                    .filter(w -> !w.equals(workflow))
                    .flatMap(w -> w.getProcessesToClose().stream())
                    .forEach(launcher::kill);
        } else {
            workflow.getProcessesToClose().forEach(launcher::kill);
        }

        for (AppEntry entry : workflow.getAppsToOpen()) {
            if (entry.getDelayMs() > 0) {
                try {
                    Thread.sleep(entry.getDelayMs());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.accept("Activation interrupted.");
                    return;
                }
            }
            launcher.launch(entry);
        }
    }

    /**
     * Persists an edited list of workflows to the backing store.
     *
     * @throws IOException if the config file cannot be written
     */
    public void save(List<Workflow> workflows) throws IOException {
        repository.save(workflows);
        logger.accept("Workflows saved successfully.");
    }

    /**
     * Deletes the config file, resetting the application to a first-run state.
     *
     * @throws IOException if the file cannot be deleted
     */
    public void resetConfig() throws IOException {
        repository.reset();
    }
}
