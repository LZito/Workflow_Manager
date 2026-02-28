package at.lzito.workflowmanager.workflow.application;

import at.lzito.workflowmanager.workflow.domain.AppEntry;

/**
 * Application-layer interface for launching and terminating OS processes.
 * Implementations live in the infrastructure layer.
 */
public interface ProcessLauncher {

    /** Launch the given entry (application or URL). */
    void launch(AppEntry entry);

    /** Terminate all OS processes matching the given name. */
    void kill(String processName);
}
