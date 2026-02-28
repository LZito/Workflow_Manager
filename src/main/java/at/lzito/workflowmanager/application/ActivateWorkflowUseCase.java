package at.lzito.workflowmanager.application;

import at.lzito.workflowmanager.application.port.ProcessLauncherPort;
import at.lzito.workflowmanager.domain.AppEntry;
import at.lzito.workflowmanager.domain.Workflow;
import at.lzito.workflowmanager.domain.WorkflowRepository;

import java.util.function.Consumer;

/**
 * Activates a workflow: optionally closes other processes, then launches each app/URL.
 * Must be called on a background thread — never on the EDT.
 */
public class ActivateWorkflowUseCase {

    private final WorkflowRepository    repository;
    private final ProcessLauncherPort   launcher;
    private final Consumer<String>      logger;

    public ActivateWorkflowUseCase(
            WorkflowRepository  repository,
            ProcessLauncherPort launcher,
            Consumer<String>    logger) {
        this.repository = repository;
        this.launcher   = launcher;
        this.logger     = logger;
    }

    /**
     * Execute the activation of {@code workflow}.
     *
     * <p>Order of operations:
     * <ol>
     *   <li>If {@code closeOthers} is set, kill processes listed in every other workflow.</li>
     *   <li>Otherwise kill processes in this workflow's own {@code processesToClose} list.</li>
     *   <li>Launch each {@link AppEntry} in order, honouring {@code delayMs}.</li>
     * </ol>
     */
    public void execute(Workflow workflow) {
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
}
