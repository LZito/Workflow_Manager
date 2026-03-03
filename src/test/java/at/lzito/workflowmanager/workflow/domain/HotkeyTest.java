package at.lzito.workflowmanager.workflow.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HotkeyTest {

    @Test
    void of_null_returnsNull() {
        assertNull(Hotkey.of(null));
    }

    @Test
    void of_blank_returnsNull() {
        assertNull(Hotkey.of(""));
        assertNull(Hotkey.of("   "));
    }

    @Test
    void of_uppercaseNormalised() {
        Hotkey h = Hotkey.of("CTRL+ALT+1");
        assertNotNull(h);
        assertEquals("ctrl+alt+1", h.getRaw());
    }

    @Test
    void of_trimmed() {
        Hotkey h = Hotkey.of("  ctrl+alt+1  ");
        assertNotNull(h);
        assertEquals("ctrl+alt+1", h.getRaw());
    }

    @Test
    void of_mixedCase_normalised() {
        Hotkey h = Hotkey.of("Ctrl+Shift+F5");
        assertNotNull(h);
        assertEquals("ctrl+shift+f5", h.getRaw());
    }

    @Test
    void equality_sameKey() {
        assertEquals(Hotkey.of("ctrl+alt+1"), Hotkey.of("ctrl+alt+1"));
    }

    @Test
    void equality_normalisedToSame() {
        assertEquals(Hotkey.of("CTRL+ALT+1"), Hotkey.of("ctrl+alt+1"));
    }

    @Test
    void notEqual_differentKeys() {
        assertNotEquals(Hotkey.of("ctrl+alt+1"), Hotkey.of("ctrl+alt+2"));
    }

    @Test
    void toString_returnsRaw() {
        assertEquals("ctrl+alt+1", Hotkey.of("ctrl+alt+1").toString());
    }
}
