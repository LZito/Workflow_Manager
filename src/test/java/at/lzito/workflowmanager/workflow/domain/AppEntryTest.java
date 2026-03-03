package at.lzito.workflowmanager.workflow.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppEntryTest {

    @Test
    void isUrl_whenUrlSet() {
        AppEntry e = new AppEntry("Browser", null, "https://example.com", null, 0);
        assertTrue(e.isUrl());
    }

    @Test
    void isUrl_whenOnlyPath() {
        AppEntry e = new AppEntry("App", "/usr/bin/app", null, null, 0);
        assertFalse(e.isUrl());
    }

    @Test
    void isUrl_whenUrlBlank() {
        AppEntry e = new AppEntry("App", "/usr/bin/app", "  ", null, 0);
        assertFalse(e.isUrl());
    }

    @Test
    void nullArgs_defaultsToEmpty() {
        AppEntry e = new AppEntry("App", "/bin/app", null, null, 0);
        assertNotNull(e.getArgs());
        assertTrue(e.getArgs().isEmpty());
    }

    @Test
    void args_isUnmodifiable() {
        AppEntry e = new AppEntry("App", "/bin/app", null, List.of("--flag"), 0);
        assertThrows(UnsupportedOperationException.class, () -> e.getArgs().clear());
    }

    @Test
    void equality_byNamePathUrlOnly_argsAndDelayIgnored() {
        AppEntry a = new AppEntry("App", "/bin/app", null, List.of("--x"), 500);
        AppEntry b = new AppEntry("App", "/bin/app", null, List.of(),       0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqual_differentPath() {
        AppEntry a = new AppEntry("App", "/bin/app1", null, null, 0);
        AppEntry b = new AppEntry("App", "/bin/app2", null, null, 0);
        assertNotEquals(a, b);
    }

    @Test
    void notEqual_differentName() {
        AppEntry a = new AppEntry("App1", "/bin/app", null, null, 0);
        AppEntry b = new AppEntry("App2", "/bin/app", null, null, 0);
        assertNotEquals(a, b);
    }
}
