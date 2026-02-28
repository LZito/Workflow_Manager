package at.lzito.workflowmanager.workflow.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate Root representing a named workflow.
 * Identity is determined solely by {@link #name}.
 */
public final class Workflow {

    private final String name;
    private final String icon;
    private final Hotkey hotkey;
    private final List<AppEntry> appsToOpen;
    private final List<String> processesToClose;
    private final boolean closeOthers;

    public Workflow(
            String name,
            String icon,
            Hotkey hotkey,
            List<AppEntry> appsToOpen,
            List<String> processesToClose,
            boolean closeOthers) {

        Objects.requireNonNull(name, "Workflow name must not be null");
        this.name             = name;
        this.icon             = icon;
        this.hotkey           = hotkey;
        this.appsToOpen       = appsToOpen != null
                ? Collections.unmodifiableList(appsToOpen)
                : Collections.emptyList();
        this.processesToClose = processesToClose != null
                ? Collections.unmodifiableList(processesToClose)
                : Collections.emptyList();
        this.closeOthers      = closeOthers;
    }

    // ── Business methods ──────────────────────────────────────────────────────

    /** Returns {@code true} if this workflow has a hotkey assigned. */
    public boolean hasHotkey() {
        return hotkey != null;
    }

    /** Returns the label to show in the UI: icon (if any) followed by the name. */
    public String displayName() {
        return icon != null && !icon.isBlank() ? icon + " " + name : name;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String         getName()             { return name; }
    public String         getIcon()             { return icon; }
    public Hotkey         getHotkey()           { return hotkey; }
    public List<AppEntry> getAppsToOpen()       { return appsToOpen; }
    public List<String>   getProcessesToClose() { return processesToClose; }
    public boolean        isCloseOthers()       { return closeOthers; }

    // ── Identity by name ──────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Workflow other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "Workflow{name='" + name + "'}";
    }
}
