package at.lzito.workflowmanager.updater.application;

import at.lzito.workflowmanager.updater.domain.Release;

import java.util.Optional;

/**
 * Output port: finds the latest published release.
 * Implementations live in the infrastructure layer and must never throw —
 * a broken network must not affect the rest of the application.
 */
public interface ReleaseRepository {

    Optional<Release> findLatest();
}
