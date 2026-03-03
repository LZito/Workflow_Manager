package at.lzito.workflowmanager.workflow.application;

import at.lzito.workflowmanager.workflow.domain.AppEntry;

/**
 * Launches and terminates OS processes on behalf of the application service.
 */
public interface ProcessLauncher {

    /** Launch the given entry (application or URL). */
    void launch(AppEntry entry);

    /** Terminate all OS processes matching the given name. */
    void kill(String processName);
}
