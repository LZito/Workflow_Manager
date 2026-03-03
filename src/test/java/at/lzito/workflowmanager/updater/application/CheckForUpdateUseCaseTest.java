package at.lzito.workflowmanager.updater.application;

import at.lzito.workflowmanager.updater.domain.Release;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class CheckForUpdateUseCaseTest {

    private ReleaseRepository releaseRepository;

    @BeforeEach
    void setUp() {
        releaseRepository = Mockito.mock(ReleaseRepository.class);
    }

    @Test
    void execute_returnsEmpty_whenNoRelease() {
        when(releaseRepository.findLatest()).thenReturn(Optional.empty());
        CheckForUpdateUseCase useCase = new CheckForUpdateUseCase(releaseRepository, "1.0.7");

        assertTrue(useCase.execute().isEmpty());
    }

    @Test
    void execute_returnsEmpty_whenSameVersion() {
        when(releaseRepository.findLatest())
                .thenReturn(Optional.of(new Release("1.0.7", "https://example.com")));
        CheckForUpdateUseCase useCase = new CheckForUpdateUseCase(releaseRepository, "1.0.7");

        assertTrue(useCase.execute().isEmpty());
    }

    @Test
    void execute_returnsEmpty_whenOlderRelease() {
        when(releaseRepository.findLatest())
                .thenReturn(Optional.of(new Release("1.0.6", "https://example.com")));
        CheckForUpdateUseCase useCase = new CheckForUpdateUseCase(releaseRepository, "1.0.7");

        assertTrue(useCase.execute().isEmpty());
    }

    @Test
    void execute_returnsRelease_whenNewer() {
        Release newer = new Release("1.0.8", "https://example.com/release");
        when(releaseRepository.findLatest()).thenReturn(Optional.of(newer));
        CheckForUpdateUseCase useCase = new CheckForUpdateUseCase(releaseRepository, "1.0.7");

        Optional<Release> result = useCase.execute();
        assertTrue(result.isPresent());
        assertEquals(newer, result.get());
    }

    @Test
    void execute_withNullCurrentVersion_defaultsTo000() {
        Release newer = new Release("1.0.0", "https://example.com");
        when(releaseRepository.findLatest()).thenReturn(Optional.of(newer));
        CheckForUpdateUseCase useCase = new CheckForUpdateUseCase(releaseRepository, null);

        // 1.0.0 is newer than default 0.0.0
        assertTrue(useCase.execute().isPresent());
    }
}
