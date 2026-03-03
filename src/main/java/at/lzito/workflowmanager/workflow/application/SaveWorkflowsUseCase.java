package at.lzito.workflowmanager.workflow.application;

import at.lzito.workflowmanager.workflow.domain.Workflow;
import at.lzito.workflowmanager.workflow.domain.WorkflowRepository;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Persists an edited list of workflows to the backing store.
 */
public class SaveWorkflowsUseCase {

    private final WorkflowRepository repository;
    private final Consumer<String>   logger;

    public SaveWorkflowsUseCase(WorkflowRepository repository, Consumer<String> logger) {
        this.repository = repository;
        this.logger     = logger;
    }

    public void execute(List<Workflow> workflows) throws IOException {
        repository.save(workflows);
        logger.accept("Workflows saved successfully.");
    }
}
