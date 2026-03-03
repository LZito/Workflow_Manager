package at.lzito.workflowmanager.workflow.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import at.lzito.workflowmanager.workflow.domain.AppEntry;
import at.lzito.workflowmanager.workflow.domain.Hotkey;
import at.lzito.workflowmanager.workflow.domain.Workflow;
import at.lzito.workflowmanager.workflow.domain.WorkflowRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    public void save(List<Workflow> workflows) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        ConfigDto dto = new ConfigDto();
        dto.workflows = workflows.stream().map(this::toDto).collect(Collectors.toList());
        mapper.writerWithDefaultPrettyPrinter().writeValue(CONFIG_FILE.toFile(), dto);
        cache = Collections.unmodifiableList(new ArrayList<>(workflows));
        logger.accept("Config saved: " + workflows.size() + " workflow(s) → " + CONFIG_FILE);
    }

    @Override
    public void reset() throws IOException {
        Files.deleteIfExists(CONFIG_FILE);
        cache = Collections.emptyList();
        logger.accept("Config reset.");
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

    private WorkflowDto toDto(Workflow w) {
        WorkflowDto dto = new WorkflowDto();
        dto.name        = w.getName();
        dto.icon        = w.getIcon();
        dto.hotkey      = w.hasHotkey() ? w.getHotkey().getRaw() : null;
        dto.open        = w.getAppsToOpen().stream().map(this::toAppEntryDto).collect(Collectors.toList());
        dto.close       = new ArrayList<>(w.getProcessesToClose());
        dto.closeOthers = w.isCloseOthers();
        return dto;
    }

    private AppEntryDto toAppEntryDto(AppEntry e) {
        AppEntryDto dto = new AppEntryDto();
        dto.name    = e.getName();
        dto.path    = e.getPath();
        dto.url     = e.getUrl();
        dto.args    = new ArrayList<>(e.getArgs());
        dto.delayMs = e.getDelayMs();
        return dto;
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
