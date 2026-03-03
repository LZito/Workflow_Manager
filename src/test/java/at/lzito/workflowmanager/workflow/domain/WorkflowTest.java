package at.lzito.workflowmanager.workflow.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowTest {

    private static Workflow simple(String name) {
        return new Workflow(name, null, null, List.of(), List.of(), false);
    }

    @Test
    void displayName_noIcon() {
        Workflow w = new Workflow("Coding", null, null, List.of(), List.of(), false);
        assertEquals("Coding", w.displayName());
    }

    @Test
    void displayName_withIcon() {
        Workflow w = new Workflow("Coding", "💻", null, List.of(), List.of(), false);
        assertEquals("💻 Coding", w.displayName());
    }

    @Test
    void displayName_blankIconTreatedAsNoIcon() {
        Workflow w = new Workflow("Coding", "  ", null, List.of(), List.of(), false);
        assertEquals("Coding", w.displayName());
    }

    @Test
    void hasHotkey_true() {
        Workflow w = new Workflow("Coding", null, Hotkey.of("ctrl+alt+1"), List.of(), List.of(), false);
        assertTrue(w.hasHotkey());
    }

    @Test
    void hasHotkey_false() {
        assertTrue(!simple("Coding").hasHotkey());
    }

    @Test
    void equalsAndHashCode_byNameOnly() {
        Workflow a = new Workflow("Coding", "💻", Hotkey.of("ctrl+1"), List.of(), List.of(), false);
        Workflow b = new Workflow("Coding", null,  null,               List.of(), List.of(), true);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqual_differentNames() {
        assertNotEquals(simple("Coding"), simple("Gaming"));
    }

    @Test
    void nullName_throws() {
        assertThrows(NullPointerException.class,
                () -> new Workflow(null, null, null, List.of(), List.of(), false));
    }

    @Test
    void appsToOpen_isUnmodifiable() {
        Workflow w = new Workflow("X", null, null,
                List.of(new AppEntry("App", "/app", null, null, 0)), List.of(), false);
        assertThrows(UnsupportedOperationException.class,
                () -> w.getAppsToOpen().clear());
    }

    @Test
    void processesToClose_isUnmodifiable() {
        Workflow w = new Workflow("X", null, null, List.of(), List.of("proc.exe"), false);
        assertThrows(UnsupportedOperationException.class,
                () -> w.getProcessesToClose().clear());
    }
}
