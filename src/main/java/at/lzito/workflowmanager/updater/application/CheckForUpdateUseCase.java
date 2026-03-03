package at.lzito.workflowmanager.updater.application;

import at.lzito.workflowmanager.updater.domain.Release;

import java.util.Optional;

/**
 * Checks whether a newer release is available than the version currently running.
 */
public class CheckForUpdateUseCase {

    private final ReleaseRepository releaseRepository;
    private final String            currentVersion;

    public CheckForUpdateUseCase(ReleaseRepository releaseRepository, String currentVersion) {
        this.releaseRepository = releaseRepository;
        this.currentVersion    = currentVersion != null ? currentVersion : "0.0.0";
    }

    /** @return the latest {@link Release} if it is strictly newer than the running version. */
    public Optional<Release> execute() {
        return releaseRepository.findLatest()
                .filter(r -> r.isNewerThan(currentVersion));
    }
}
