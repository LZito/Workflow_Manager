package at.lzito.workflowmanager.workflow.infrastructure.persistence;

import at.lzito.workflowmanager.workflow.domain.AppEntry;
import at.lzito.workflowmanager.workflow.domain.Hotkey;
import at.lzito.workflowmanager.workflow.domain.Workflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonWorkflowRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonWorkflowRepository repo(String filename) {
        return new JsonWorkflowRepository(tempDir.resolve(filename), msg -> {});
    }

    // ── First-run default config ───────────────────────────────────────────────

    @Test
    void reload_createsDefaultConfig_onFirstRun() throws IOException {
        JsonWorkflowRepository repository = repo("workflows.json");
        repository.reload();

        List<Workflow> all = repository.findAll();
        assertFalse(all.isEmpty(), "Default config should contain at least one workflow");
        // default workflow is "Coding"
        assertEquals("Coding", all.get(0).getName());
    }

    @Test
    void reload_createsJsonFileOnDisk_onFirstRun() throws IOException {
        Path configFile = tempDir.resolve("workflows.json");
        JsonWorkflowRepository repository = new JsonWorkflowRepository(configFile, msg -> {});
        assertFalse(Files.exists(configFile));

        repository.reload();

        assertTrue(Files.exists(configFile));
    }

    // ── Round-trip ────────────────────────────────────────────────────────────

    @Test
    void roundTrip_saveAndReload_preservesAllFields() throws IOException {
        JsonWorkflowRepository repository = repo("workflows.json");
        AppEntry app = new AppEntry("VS Code", "/usr/bin/code", null, List.of("--new-window"), 100);
        Workflow original = new Workflow(
                "Coding", "💻", Hotkey.of("ctrl+alt+1"),
                List.of(app), List.of("code.exe"), true);

        repository.save(List.of(original));

        JsonWorkflowRepository fresh = repo("workflows.json");
        fresh.reload();
        List<Workflow> loaded = fresh.findAll();

        assertEquals(1, loaded.size());
        Workflow w = loaded.get(0);
        assertEquals("Coding",     w.getName());
        assertEquals("💻",         w.getIcon());
        assertTrue(w.hasHotkey());
        assertEquals("ctrl+alt+1", w.getHotkey().getRaw());
        assertTrue(w.isCloseOthers());
        assertEquals(List.of("code.exe"), w.getProcessesToClose());

        assertEquals(1, w.getAppsToOpen().size());
        AppEntry a = w.getAppsToOpen().get(0);
        assertEquals("VS Code",        a.getName());
        assertEquals("/usr/bin/code",  a.getPath());
        assertEquals(List.of("--new-window"), a.getArgs());
        assertEquals(100,              a.getDelayMs());
    }

    @Test
    void roundTrip_withHotkey() throws IOException {
        JsonWorkflowRepository repository = repo("workflows.json");
        Workflow w = new Workflow("Gaming", null, Hotkey.of("ctrl+alt+2"),
                List.of(), List.of(), false);

        repository.save(List.of(w));

        JsonWorkflowRepository fresh = repo("workflows.json");
        fresh.reload();
        Workflow loaded = fresh.findAll().get(0);

        assertTrue(loaded.hasHotkey());
        assertEquals("ctrl+alt+2", loaded.getHotkey().getRaw());
    }

    @Test
    void roundTrip_withUrlEntry() throws IOException {
        JsonWorkflowRepository repository = repo("workflows.json");
        AppEntry url = new AppEntry("GitHub", null, "https://github.com", null, 0);
        Workflow w   = new Workflow("Dev", null, null, List.of(url), List.of(), false);

        repository.save(List.of(w));

        JsonWorkflowRepository fresh = repo("workflows.json");
        fresh.reload();
        AppEntry loaded = fresh.findAll().get(0).getAppsToOpen().get(0);

        assertTrue(loaded.isUrl());
        assertEquals("https://github.com", loaded.getUrl());
    }

    @Test
    void roundTrip_withNullHotkey() throws IOException {
        JsonWorkflowRepository repository = repo("workflows.json");
        Workflow w = new Workflow("Work", null, null, List.of(), List.of(), false);

        repository.save(List.of(w));

        JsonWorkflowRepository fresh = repo("workflows.json");
        fresh.reload();
        assertFalse(fresh.findAll().get(0).hasHotkey());
    }

    // ── reset ─────────────────────────────────────────────────────────────────

    @Test
    void reset_deletesConfigFile() throws IOException {
        JsonWorkflowRepository repository = repo("workflows.json");
        repository.reload();                    // creates default file
        assertTrue(Files.exists(repository.configPath()));

        repository.reset();

        assertFalse(Files.exists(repository.configPath()));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void reset_whenFileDoesNotExist_doesNotThrow() {
        JsonWorkflowRepository repository = repo("workflows.json");
        assertDoesNotThrow(repository::reset);
    }

    // ── configPath ────────────────────────────────────────────────────────────

    @Test
    void configPath_returnsExpectedPath() {
        Path configFile = tempDir.resolve("workflows.json");
        JsonWorkflowRepository repository = new JsonWorkflowRepository(configFile, msg -> {});
        assertEquals(configFile, repository.configPath());
    }

    // ── multiple workflows ────────────────────────────────────────────────────

    @Test
    void roundTrip_multipleWorkflows_preservesOrder() throws IOException {
        JsonWorkflowRepository repository = repo("workflows.json");
        List<Workflow> original = List.of(
                new Workflow("Alpha", null, null, List.of(), List.of(), false),
                new Workflow("Beta",  null, null, List.of(), List.of(), false),
                new Workflow("Gamma", null, null, List.of(), List.of(), false)
        );

        repository.save(original);

        JsonWorkflowRepository fresh = repo("workflows.json");
        fresh.reload();
        List<Workflow> loaded = fresh.findAll();

        assertEquals(3, loaded.size());
        assertEquals("Alpha", loaded.get(0).getName());
        assertEquals("Beta",  loaded.get(1).getName());
        assertEquals("Gamma", loaded.get(2).getName());
    }
}
