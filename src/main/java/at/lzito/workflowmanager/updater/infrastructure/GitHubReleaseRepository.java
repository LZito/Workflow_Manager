package at.lzito.workflowmanager.updater.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import at.lzito.workflowmanager.updater.application.ReleaseRepository;
import at.lzito.workflowmanager.updater.domain.Release;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * {@link ReleaseRepository} backed by the GitHub Releases API.
 *
 * <p>All failures are swallowed and result in {@code Optional.empty()} so a broken
 * network never bubbles up into the rest of the application.
 */
public class GitHubReleaseRepository implements ReleaseRepository {

    private static final String API = "https://api.github.com/repos/%s/%s/releases/latest";

    private final String       owner;
    private final String       repo;
    private final String       currentVersion;
    private final ObjectMapper mapper = new ObjectMapper();

    public GitHubReleaseRepository(String owner, String repo, String currentVersion) {
        this.owner          = owner;
        this.repo           = repo;
        this.currentVersion = currentVersion;
    }

    @Override
    public Optional<Release> findLatest() {
        if (owner == null || owner.isBlank()) return Optional.empty();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(API, owner, repo)))
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "WorkflowManager/" + currentVersion)
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return Optional.empty();

            JsonNode root    = mapper.readTree(response.body());
            String   tagName = root.path("tag_name").asText("");
            if (tagName.isEmpty()) return Optional.empty();

            String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;

            for (JsonNode asset : root.path("assets")) {
                if (asset.path("name").asText("").endsWith(".jar")) {
                    String url = asset.path("browser_download_url").asText("");
                    if (!url.isEmpty()) return Optional.of(new Release(version, url));
                }
            }
        } catch (Exception ignored) {
            // Network unavailable, rate-limited, malformed response — not a problem
        }
        return Optional.empty();
    }
}
