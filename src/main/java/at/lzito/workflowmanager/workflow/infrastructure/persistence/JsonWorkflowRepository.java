package at.lzito.workflowmanager.workflow.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import at.lzito.workflowmanager.workflow.domain.AppEntry;
import at.lzito.workflowmanager.workflow.domain.Hotkey;
import at.lzito.workflowmanager.workflow.domain.Workflow;
import at.lzito.workflowmanager.workflow.domain.WorkflowRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * WorkflowRepository implementation backed by a JSON file.
 *
 * <p>Jackson DTOs are private inner classes — they never escape this class.
 * The domain layer has zero knowledge of Jackson.
 */
public class JsonWorkflowRepository implements WorkflowRepository {

    private static final Path CONFIG_DIR  = Path.of(System.getProperty("user.home"), ".workflow-manager");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("workflows.json");

    private static final String DEFAULT_CONFIG = """
            {
              "workflows": [
                {
                  "name": "Coding",
                  "icon": "💻",
                  "hotkey": "ctrl+alt+1",
                  "open": [
                    { "name": "VS Code", "path": "code", "args": [], "delayMs": 0 }
                  ],
                  "close": [],
                  "closeOthers": false
                }
              ]
            }
            """;

    private final ObjectMapper     mapper;
    private final Consumer<String> logger;
    private List<Workflow>         cache = Collections.emptyList();

    public JsonWorkflowRepository(Consumer<String> logger) {
        this.mapper = new ObjectMapper();
        this.logger = logger;
    }

    // ── WorkflowRepository ────────────────────────────────────────────────────

    @Override
    public List<Workflow> findAll() {
        return cache;
    }

    @Override
    public void reload() throws IOException {
        ensureConfigExists();
        ConfigDto dto = mapper.readValue(CONFIG_FILE.toFile(), ConfigDto.class);
        cache = dto.workflows == null
                ? Collections.emptyList()
                : dto.workflows.stream()
                        .map(this::toDomain)
                        .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Path configPath() {
        return CONFIG_FILE;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void ensureConfigExists() throws IOException {
        if (!Files.exists(CONFIG_FILE)) {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(CONFIG_FILE, DEFAULT_CONFIG);
            logger.accept("Created default config at: " + CONFIG_FILE);
        }
    }

    private Workflow toDomain(WorkflowDto dto) {
        List<AppEntry> entries = dto.open == null
                ? Collections.emptyList()
                : dto.open.stream().map(this::toAppEntry).collect(Collectors.toList());

        List<String> toClose = dto.close != null ? dto.close : Collections.emptyList();

        return new Workflow(
                dto.name,
                dto.icon,
                Hotkey.of(dto.hotkey),
                entries,
                toClose,
                dto.closeOthers);
    }

    private AppEntry toAppEntry(AppEntryDto dto) {
        return new AppEntry(dto.name, dto.path, dto.url, dto.args, dto.delayMs);
    }

    // ── Private DTOs (Jackson-only, never exposed outside this class) ─────────

    private static class ConfigDto {
        public List<WorkflowDto> workflows;
    }

    private static class WorkflowDto {
        public String            name;
        public String            icon;
        public String            hotkey;
        public List<AppEntryDto> open;
        public List<String>      close;
        public boolean           closeOthers;
    }

    private static class AppEntryDto {
        public String       name;
        public String       path;
        public String       url;
        public List<String> args;
        public int          delayMs;
    }
}
