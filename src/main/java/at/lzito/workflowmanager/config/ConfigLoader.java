package at.lzito.workflowmanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import at.lzito.workflowmanager.model.WorkflowConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {

    private static final Path CONFIG_DIR  = Path.of(System.getProperty("user.home"), ".workflow-manager");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("workflows.json");

    private final ObjectMapper mapper = new ObjectMapper();

    public WorkflowConfig load() throws IOException {
        if (!Files.exists(CONFIG_FILE)) {
            createDefault();
        }
        return mapper.readValue(CONFIG_FILE.toFile(), WorkflowConfig.class);
    }

    public void save(WorkflowConfig config) throws IOException {
        Files.createDirectories(CONFIG_DIR);
        mapper.writerWithDefaultPrettyPrinter().writeValue(CONFIG_FILE.toFile(), config);
    }

    private void createDefault() throws IOException {
        Files.createDirectories(CONFIG_DIR);
        String defaultJson = """
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
        Files.writeString(CONFIG_FILE, defaultJson);
    }

    public Path getConfigFile() {
        return CONFIG_FILE;
    }
}
