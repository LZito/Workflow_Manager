package at.lzito.workflowmanager.domain;

import java.util.Collections;
import java.util.List;

/** Value Object: an application or URL to open as part of a workflow. */
public final class AppEntry {

    private final String name;
    private final String path;
    private final String url;
    private final List<String> args;
    private final int delayMs;

    public AppEntry(String name, String path, String url, List<String> args, int delayMs) {
        this.name    = name;
        this.path    = path;
        this.url     = url;
        this.args    = args != null ? Collections.unmodifiableList(args) : Collections.emptyList();
        this.delayMs = delayMs;
    }

    /** Returns {@code true} when this entry should be opened as a URL in the browser. */
    public boolean isUrl() {
        return url != null && !url.isBlank();
    }

    public String getName()      { return name; }
    public String getPath()      { return path; }
    public String getUrl()       { return url; }
    public List<String> getArgs() { return args; }
    public int getDelayMs()      { return delayMs; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppEntry other)) return false;
        return java.util.Objects.equals(name, other.name)
            && java.util.Objects.equals(path, other.path)
            && java.util.Objects.equals(url,  other.url);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, path, url);
    }

    @Override
    public String toString() {
        return "AppEntry{name='" + name + "', url=" + (isUrl() ? url : path) + "}";
    }
}
