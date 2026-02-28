package at.lzito.workflowmanager.workflow.domain;

import java.util.Objects;

/**
 * Value Object representing a keyboard shortcut.
 * Stored in normalised form: lowercase + trimmed, e.g. {@code "ctrl+alt+1"}.
 */
public final class Hotkey {

    private final String raw;

    private Hotkey(String raw) {
        this.raw = raw;
    }

    /**
     * Factory — normalises the raw string (lowercase, trimmed) before wrapping.
     *
     * @param raw the hotkey string as read from config, e.g. {@code "Ctrl+Alt+1"}
     * @return a Hotkey instance, or {@code null} if raw is null or blank
     */
    public static Hotkey of(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return new Hotkey(raw.trim().toLowerCase());
    }

    /** Returns the normalised hotkey string, e.g. {@code "ctrl+alt+1"}. */
    public String getRaw() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hotkey other)) return false;
        return Objects.equals(raw, other.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(raw);
    }

    @Override
    public String toString() {
        return raw;
    }
}
