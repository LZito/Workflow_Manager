package at.lzito.workflowmanager.domain;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Output port (repository interface) for persisting and retrieving workflows.
 * Implementations live in the infrastructure layer.
 */
public interface WorkflowRepository {

    /** Returns the currently loaded list of workflows. */
    List<Workflow> findAll();

    /** Re-reads the backing store (e.g. the JSON file) from disk. */
    void reload() throws IOException;

    /** Returns the path to the configuration file on disk. */
    Path configPath();
}
