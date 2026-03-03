package at.lzito.workflowmanager.workflow.domain;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Repository interface for persisting and retrieving workflows.
 * Implementations live in the infrastructure layer.
 */
public interface WorkflowRepository {

    /** Returns the currently loaded list of workflows. */
    List<Workflow> findAll();

    /** Re-reads the backing store (e.g. the JSON file) from disk. */
    void reload() throws IOException;

    /** Persists the given workflow list to the backing store. */
    void save(List<Workflow> workflows) throws IOException;

    /** Deletes the backing config file so the next reload recreates defaults. */
    void reset() throws IOException;

    /** Returns the path to the configuration file on disk. */
    Path configPath();
}
