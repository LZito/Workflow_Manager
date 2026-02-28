package at.lzito.workflowmanager.application.port;

import at.lzito.workflowmanager.domain.AppEntry;

/**
 * Output port for launching and terminating OS processes.
 * Implementations live in the infrastructure layer.
 */
public interface ProcessLauncherPort {

    /** Launch the given entry (application or URL). */
    void launch(AppEntry entry);

    /** Terminate all OS processes matching the given name. */
    void kill(String processName);
}
