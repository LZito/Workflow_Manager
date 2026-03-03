# WorkflowManager — Developer Guide

## Architecture

WorkflowManager follows **Domain-Driven Design** with two bounded contexts
(`workflow` and `updater`) and a strict layered architecture inside each.

```
┌──────────────────────────────────────────────────┐
│  presentation/        (Swing)                    │
│    depends on: application only                  │
└────────────────────┬─────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────┐
│  application/         (service + port interfaces) │
│    depends on: domain only                        │
└──────┬───────────────────────────────────┬────────┘
       │                                   │
┌──────▼───────┐           ┌───────────────▼────────┐
│  domain/     │           │  infrastructure/        │
│  (pure Java) │           │  (implements ports)     │
│  no deps     │           │  Jackson, JNativeHook   │
└──────────────┘           └────────────────────────┘
```

### Layer import rules (strictly enforced)

| Layer | May import | Must NOT import |
|---|---|---|
| `domain` | JDK only | Everything else |
| `application` | `domain`, JDK | `infrastructure`, `presentation`, 3rd-party |
| `infrastructure` | `application`, `domain`, JDK, 3rd-party libs | `presentation` |
| `presentation` | `application`, `domain`, JDK, Swing, FlatLaf | `infrastructure` |

`App.java` (composition root) is the **only** class allowed to import across all layers.

---

## Actual package structure

```
at.lzito.workflowmanager/
├── App.java                                      ← composition root (wires everything)
│
├── workflow/                                     ← bounded context: core feature
│   ├── domain/
│   │   ├── Workflow.java                         ← Aggregate Root; identity = name
│   │   ├── AppEntry.java                         ← Value Object (immutable)
│   │   ├── Hotkey.java                           ← Value Object (normalised lowercase)
│   │   └── WorkflowRepository.java               ← output port (interface)
│   ├── application/
│   │   ├── WorkflowAppService.java               ← single app service (not separate use cases)
│   │   ├── HotkeyRegistry.java                   ← output port (interface)
│   │   └── ProcessLauncher.java                  ← output port (interface)
│   ├── infrastructure/
│   │   ├── persistence/JsonWorkflowRepository.java  ← Jackson impl; private inner DTOs
│   │   ├── process/OsProcessLauncher.java           ← taskkill (Win) / pkill (Unix)
│   │   └── hotkey/JNativeHookRegistry.java          ← HotkeyRegistry + NativeKeyListener
│   └── presentation/
│       ├── MainWindow.java                       ← JFrame; WrapLayout for buttons
│       ├── ConfigEditorDialog.java               ← full CRUD editor for workflows
│       ├── AppEntryEditorDialog.java             ← modal for a single app/URL entry
│       └── InstalledAppPickerDialog.java         ← file picker for installed executables
│
└── updater/                                      ← bounded context: self-update
    ├── domain/
    │   └── Release.java                          ← Value Object; identity = version; semver compare
    ├── application/
    │   ├── ReleaseRepository.java                ← output port (interface)
    │   └── CheckForUpdateUseCase.java            ← returns Optional<Release> if newer
    └── infrastructure/
        ├── GitHubReleaseRepository.java          ← GitHub Releases API; all errors → empty
        └── JarSelfUpdater.java                   ← downloads JAR; PowerShell/sh swap script
```

> **Note:** Port interfaces live directly in `application/`, not in a `port/` sub-package.
> `WorkflowAppService` is a single coordinating service, not split into separate use-case classes.

---

## Build commands

```bash
gradle compileJava       # compile only (fastest feedback)
gradle test              # run unit tests (61 tests across 7 classes)
gradle shadowJar         # fat JAR → /tmp/wm-build/libs/workflow-manager.jar  (WSL)
                         #           build/libs/workflow-manager.jar           (Windows CI)
gradle build             # compile + test + fat JAR
gradle installGitHooks   # one-time setup after cloning: points Git at .githooks/
```

> **WSL / NTFS:** `gradlew` is not executable on NTFS — use `gradle` directly from WSL.
> Build output is redirected to `/tmp/wm-build/` on Linux (configured in `build.gradle.kts`).

---

## Key configuration files

### `gradle.properties`

```properties
systemProp.org.gradle.native=false   # disables Gradle's native POSIX layer (required on WSL/NTFS)
projectVersion=1.0.7                 # updated automatically by gradle release
```

### `src/main/resources/app.properties`

```properties
github.owner=LZito          # GitHub username; leave blank to disable the auto-update check
github.repo=Workflow_Manager
```

Read by `App.java` at startup to configure `GitHubReleaseRepository`. If `github.owner`
is blank, `findLatest()` returns `Optional.empty()` immediately without any network call.

---

## Git hooks

`.githooks/pre-push` runs `gradle test` before every push. Activate once after cloning:

```bash
gradle installGitHooks   # sets core.hooksPath=.githooks
```

The `release` task calls `git push` via `ProcessBuilder`, so the hook fires automatically
during a release — tests must pass before any release commit goes out.

### WSL/NTFS gotchas for hook files

- **Never edit `.githooks/` scripts with a Windows editor or the Write tool** — they add
  CRLF line endings. Linux `exec()` reads the shebang as `#!/bin/sh\r`, looks for an
  interpreter named `/bin/sh` + carriage-return, and fails with `No such file or directory`.
- Always write hook scripts from WSL using a heredoc: `cat > .githooks/hook << 'EOF'`
- `git config core.hooksPath` also fails on NTFS (can't chmod the lock file). The
  `installGitHooks` task handles this by writing `.git/config` directly as a fallback.

---

## App startup sequence (`App.java`)

```
1. FlatDarkLaf.setup()
2. Build shared logger (AtomicReference, starts as System.out::println)
3. Construct infrastructure: JsonWorkflowRepository, OsProcessLauncher, JNativeHookRegistry
4. Construct WorkflowAppService
5. Detect firstRun = !Files.exists(repository.configPath())   ← BEFORE reload
6. hotkeyRegistry.register()  +  shutdown hook for unregister
7. Construct GitHubReleaseRepository + CheckForUpdateUseCase
8. SwingUtilities.invokeLater:
   a. new MainWindow(workflowAppService, firstRun)   ← triggers reload internally
   b. logRef rewired to window::log
   c. Daemon thread: sleep 4 s → checkForUpdate.execute() → window.promptUpdate(...)
      All exceptions silently swallowed — network failures must not affect the app
```

`firstRun=true` causes `MainWindow` to open `ConfigEditorDialog` immediately after
initialization, guiding new users to add their first workflow.

---

## Testing

### Test layout (mirrors main source tree)

```
src/test/java/at/lzito/workflowmanager/
├── workflow/
│   ├── domain/
│   │   ├── WorkflowTest.java           (10 tests)
│   │   ├── AppEntryTest.java           (8 tests)
│   │   └── HotkeyTest.java             (9 tests)
│   ├── application/
│   │   └── WorkflowAppServiceTest.java (9 tests — Mockito)
│   └── infrastructure/persistence/
│       └── JsonWorkflowRepositoryTest.java (10 tests — @TempDir)
└── updater/
    ├── domain/
    │   └── ReleaseTest.java            (10 tests)
    └── application/
        └── CheckForUpdateUseCaseTest.java (5 tests — Mockito)
```

### Testing conventions

| Scenario | Pattern |
|---|---|
| Domain value objects | Plain JUnit 5, no mocks needed |
| Application service / use case | Mockito — mock all port interfaces |
| Infrastructure (file I/O) | `@TempDir` — never touch `~/.workflow-manager/` in tests |
| EDT / Swing | Not tested; keep presentation logic minimal |

**`JsonWorkflowRepository` test constructor:** package-private constructor accepts an
explicit `Path configFile` — use with `@TempDir` for full isolation:

```java
@TempDir Path tempDir;
var repo = new JsonWorkflowRepository(tempDir.resolve("workflows.json"), msg -> {});
```

---

## Adding a new feature

1. **Define the capability** in `domain/` (new concept) or add a port interface in
   `application/` (new output boundary, e.g. notifications).
2. **Implement the port** in `infrastructure/` using the concrete library.
3. **Wire it up** in `App.java` only — pass the implementation via constructor injection.
4. **Expose it to the UI** through `WorkflowAppService` or a new use case; `presentation/`
   must never import an `infrastructure/` class.

---

## Hotkey format

Stored in config as lowercase, `+`-separated strings:

```
ctrl+alt+1
ctrl+shift+f5
```

Modifier ordering (canonical): `ctrl` → `alt` → `shift` → `meta`.

`JNativeHookRegistry.buildCombo()` produces strings in this exact order so that
they match the normalised keys stored by `Hotkey.of()`.

---

## Key conventions

| Convention | Rule |
|---|---|
| Composition root | `App.java` only — no business logic, just wiring |
| Jackson DTOs | Private inner classes in `JsonWorkflowRepository`; never escape to domain |
| EDT safety | Swing calls via `SwingUtilities.invokeLater`; activation on background thread |
| Logger | `Consumer<String>` passed via constructor; `App.java` uses `AtomicReference` to rewire it from `System.out::println` to `window::log` after the window is created |
| Hotkey callback | `MainWindow.onHotkeyActivated()` dispatches to a new background thread; also shows a `TrayIcon` notification |
| `closeOthers` semantics | `false` → kill only this workflow's own `close` list; `true` → kill every *other* workflow's `close` list (this workflow's own `close` list is ignored) |
| Immutability | `Workflow`, `AppEntry`, `Hotkey` — final fields, `Collections.unmodifiableList` copies |
| `WorkflowRepository` contract | `findAll()`, `reload()`, `save(List<Workflow>)`, `reset()`, `configPath()` |
| Update check resilience | `GitHubReleaseRepository.findLatest()` catches all exceptions → `Optional.empty()`; a broken network must never affect the rest of the app |

---

## WSL / NTFS reference

| Problem | Cause | Fix |
|---|---|---|
| `gradlew` not executable | NTFS has no Unix execute bit | Use `gradle <task>` directly from WSL |
| `shadowJar` / `processResources` chmod errors | Gradle writes to NTFS | Build dir redirected to `/tmp/wm-build` in `build.gradle.kts` |
| `git config` fails (exit 4) | Can't chmod `.git/config.lock` on NTFS | Use `gradle installGitHooks` (has `.git/config` direct-edit fallback) |
| Hook: `No such file or directory` | Shell script has CRLF endings — shebang becomes `#!/bin/sh\r` | Rewrite with `cat > file << 'EOF'` from WSL |
| `git push` shows chmod warning | Non-fatal NTFS noise | Safe to ignore — push still succeeds |
