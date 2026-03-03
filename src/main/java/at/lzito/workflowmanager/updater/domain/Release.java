package at.lzito.workflowmanager.updater.domain;

import java.util.Objects;

/**
 * Value Object representing a published application release.
 * Identity is determined solely by {@link #version}.
 */
public final class Release {

    private final String version;
    private final String downloadUrl;

    public Release(String version, String downloadUrl) {
        this.version     = Objects.requireNonNull(version,     "version");
        this.downloadUrl = Objects.requireNonNull(downloadUrl, "downloadUrl");
    }

    public String getVersion()     { return version; }
    public String getDownloadUrl() { return downloadUrl; }

    /**
     * Returns {@code true} if this release is strictly newer than {@code otherVersion}
     * using semantic-version (major.minor.patch) comparison.
     */
    public boolean isNewerThan(String otherVersion) {
        int[] mine  = semver(this.version);
        int[] their = semver(otherVersion);
        for (int i = 0; i < 3; i++) {
            if (mine[i] > their[i]) return true;
            if (mine[i] < their[i]) return false;
        }
        return false;
    }

    private static int[] semver(String v) {
        String[] parts = v.replaceAll("[^0-9.]", "").split("\\.");
        int[] n = new int[3];
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            try { n[i] = Integer.parseInt(parts[i]); } catch (NumberFormatException ignored) {}
        }
        return n;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Release r)) return false;
        return Objects.equals(version, r.version);
    }

    @Override public int    hashCode()  { return Objects.hashCode(version); }
    @Override public String toString()  { return "Release{version='" + version + "'}"; }
}
