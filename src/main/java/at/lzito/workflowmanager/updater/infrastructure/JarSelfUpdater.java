package at.lzito.workflowmanager.updater.infrastructure;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Downloads a new release JAR and hot-swaps it after the current process exits.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Download the new JAR next to the running JAR as {@code workflow-manager-update.jar}.</li>
 *   <li>Write a tiny platform script that waits a few seconds, moves the file into place,
 *       then relaunches the app.</li>
 *   <li>Start the script and call {@link System#exit(0)}.</li>
 * </ol>
 *
 * <p>When not running from a JAR (e.g. IDE), falls back to opening the releases page.
 */
public class JarSelfUpdater {

    private JarSelfUpdater() {}

    /**
     * Applies the update. Calls {@code System.exit(0)} on success.
     *
     * @throws IOException if the download or script creation fails
     */
    public static void apply(String downloadUrl) throws IOException {
        Path currentJar = runningJarPath();
        if (currentJar == null) {
            // Not running from a JAR — open the releases page instead
            openUrl(releasePageUrl(downloadUrl));
            return;
        }

        Path updateJar = currentJar.resolveSibling("workflow-manager-update.jar");

        // ── Download ──────────────────────────────────────────────────────────
        try (InputStream in = new URL(downloadUrl).openStream()) {
            Files.copy(in, updateJar, StandardCopyOption.REPLACE_EXISTING);
        }

        // ── Write + launch platform swap script ───────────────────────────────
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (windows) {
            launchWindowsUpdater(currentJar, updateJar);
        } else {
            launchUnixUpdater(currentJar, updateJar);
        }

        System.exit(0);
    }

    // ── Platform scripts ──────────────────────────────────────────────────────

    private static void launchWindowsUpdater(Path target, Path update) throws IOException {
        Path script = target.resolveSibling("wm-update.bat");
        // timeout /t 3 waits 3 s without requiring network (unlike ping)
        String bat =
            "@echo off\r\n" +
            "timeout /t 3 /nobreak > nul\r\n" +
            "move /y \"" + update.toAbsolutePath() + "\" \"" + target.toAbsolutePath() + "\"\r\n" +
            "start javaw -jar \"" + target.toAbsolutePath() + "\"\r\n" +
            "del \"%~f0\"\r\n";
        Files.writeString(script, bat);
        new ProcessBuilder("cmd.exe", "/c", "start", "/min", script.toString()).start();
    }

    private static void launchUnixUpdater(Path target, Path update) throws IOException {
        Path script = target.resolveSibling("wm-update.sh");
        String sh =
            "#!/bin/sh\n" +
            "sleep 3\n" +
            "mv -f '" + update.toAbsolutePath() + "' '" + target.toAbsolutePath() + "'\n" +
            "java -jar '" + target.toAbsolutePath() + "' &\n" +
            "rm -- \"$0\"\n";
        Files.writeString(script, sh);
        script.toFile().setExecutable(true);
        new ProcessBuilder("sh", script.toString()).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the Path of the running JAR, or {@code null} if not running from a JAR. */
    private static Path runningJarPath() {
        try {
            Path p = Path.of(JarSelfUpdater.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isRegularFile(p) ? p : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Strips the asset download path to get the releases page URL. */
    private static String releasePageUrl(String downloadUrl) {
        // https://github.com/user/repo/releases/download/v1.1/file.jar
        //   → https://github.com/user/repo/releases/latest
        return downloadUrl.replaceAll("/releases/download/.*", "/releases/latest");
    }

    private static void openUrl(String url) throws IOException {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (Exception e) {
                throw new IOException("Cannot open browser: " + e.getMessage(), e);
            }
        }
    }
}
