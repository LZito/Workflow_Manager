package at.lzito.workflowmanager.workflow.application;

import at.lzito.workflowmanager.workflow.domain.AppEntry;
import at.lzito.workflowmanager.workflow.domain.Hotkey;
import at.lzito.workflowmanager.workflow.domain.Workflow;
import at.lzito.workflowmanager.workflow.domain.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowAppServiceTest {

    private WorkflowRepository repository;
    private ProcessLauncher     launcher;
    private HotkeyRegistry      hotkeyRegistry;
    private List<String>        logMessages;
    private WorkflowAppService  service;

    @BeforeEach
    void setUp() {
        repository     = Mockito.mock(WorkflowRepository.class);
        launcher       = Mockito.mock(ProcessLauncher.class);
        hotkeyRegistry = Mockito.mock(HotkeyRegistry.class);
        logMessages    = new ArrayList<>();
        service        = new WorkflowAppService(repository, launcher, hotkeyRegistry, logMessages::add);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_delegatesToRepository() {
        List<Workflow> expected = List.of(workflow("Coding"));
        when(repository.findAll()).thenReturn(expected);

        assertEquals(expected, service.getAll());
        verify(repository).findAll();
    }

    // ── reload ────────────────────────────────────────────────────────────────

    @Test
    void reload_callsRepositoryThenBindsHotkeys() throws IOException {
        List<Workflow> workflows = List.of(workflow("Coding"));
        when(repository.findAll()).thenReturn(workflows);
        when(repository.configPath()).thenReturn(Path.of("/tmp/workflows.json"));
        Consumer<Workflow> cb = w -> {};

        List<Workflow> result = service.reload(cb);

        verify(repository).reload();
        verify(hotkeyRegistry).bindWorkflows(workflows, cb);
        assertEquals(workflows, result);
    }

    // ── activate ─────────────────────────────────────────────────────────────

    @Test
    void activate_logsWorkflowName() {
        service.activate(workflow("Coding"));
        assertTrue(logMessages.stream().anyMatch(m -> m.contains("Coding")));
    }

    @Test
    void activate_killsOnlyOwnProcesses_whenCloseOthersFalse() {
        Workflow coding = workflowWithClose("Coding", List.of("code.exe"), false);
        service.activate(coding);

        verify(launcher).kill("code.exe");
        verifyNoMoreInteractions(launcher);
    }

    @Test
    void activate_killsAllOtherWorkflowProcesses_whenCloseOthersTrue() {
        Workflow coding  = workflowWithClose("Coding",  List.of("code.exe"),    true);
        Workflow gaming  = workflowWithClose("Gaming",  List.of("game.exe"),    false);
        Workflow browser = workflowWithClose("Browser", List.of("firefox.exe"), false);

        when(repository.findAll()).thenReturn(List.of(coding, gaming, browser));

        service.activate(coding);

        verify(launcher).kill("game.exe");
        verify(launcher).kill("firefox.exe");
        verify(launcher, never()).kill("code.exe");
    }

    @Test
    void activate_launchesAppsInOrder() {
        AppEntry app1 = new AppEntry("App1", "/bin/app1", null, null, 0);
        AppEntry app2 = new AppEntry("App2", "/bin/app2", null, null, 0);
        Workflow w = new Workflow("Work", null, null, List.of(app1, app2), List.of(), false);

        service.activate(w);

        InOrder order = inOrder(launcher);
        order.verify(launcher).launch(app1);
        order.verify(launcher).launch(app2);
    }

    @Test
    void activate_noLaunch_whenNoApps() {
        service.activate(workflow("Empty"));
        verify(launcher, never()).launch(any());
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    void save_delegatesToRepository_andLogs() throws IOException {
        List<Workflow> workflows = List.of(workflow("Coding"));
        service.save(workflows);

        verify(repository).save(workflows);
        assertTrue(logMessages.stream().anyMatch(m -> m.toLowerCase().contains("saved")));
    }

    // ── resetConfig ───────────────────────────────────────────────────────────

    @Test
    void resetConfig_delegatesToRepository() throws IOException {
        service.resetConfig();
        verify(repository).reset();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Workflow workflow(String name) {
        return new Workflow(name, null, null, List.of(), List.of(), false);
    }

    private static Workflow workflowWithClose(String name, List<String> toClose, boolean closeOthers) {
        return new Workflow(name, null, null, List.of(), toClose, closeOthers);
    }
}
