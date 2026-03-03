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
│   │   ├── HotkeyRegistry.java                   ← output port (interface) — lives here, NOT in port/
│   │   └── ProcessLauncher.java                  ← output port (interface) — lives here, NOT in port/
│   ├── infrastructure/
│   │   ├── persistence/JsonWorkflowRepository.java  ← Jackson impl; private inner DTOs
│   │   ├── process/OsProcessLauncher.java           ← ProcessLauncher impl
│   │   └── hotkey/JNativeHookRegistry.java          ← HotkeyRegistry + NativeKeyListener
│   └── presentation/
│       ├── MainWindow.java                       ← JFrame (not ui/, not MainWindow in root)
│       ├── ConfigEditorDialog.java
│       ├── AppEntryEditorDialog.java
│       └── InstalledAppPickerDialog.java
│
└── updater/                                      ← bounded context: self-update
    ├── domain/
    │   └── Release.java                          ← Value Object; identity = version; semver compare
    ├── application/
    │   ├── ReleaseRepository.java                ← output port (interface)
    │   └── CheckForUpdateUseCase.java            ← returns Optional<Release> if newer
    └── infrastructure/
        ├── GitHubReleaseRepository.java          ← GitHub API impl
        └── JarSelfUpdater.java                   ← downloads + replaces running JAR
```

> **Note:** The original design doc used `ui/` and `application/port/` — the actual code uses
> `presentation/` and ports live directly in `application/`. `WorkflowAppService` is a single
> coordinating service, not split into separate use case classes.

---

## Build commands

```bash
gradle compileJava       # compile only (fastest feedback)
gradle test              # run unit tests (61 tests across 7 classes)
gradle shadowJar         # fat JAR → /tmp/wm-build/libs/workflow-manager.jar
gradle build             # compile + test + fat JAR
gradle installGitHooks   # one-time setup: point Git at .githooks/ (run after cloning)
```

> **WSL / NTFS:** `gradlew` is not executable on NTFS — use `gradle` directly from WSL.
> Build output is redirected to `/tmp/wm-build/` on Linux (see `build.gradle.kts`).
> The final JAR is always at `/tmp/wm-build/libs/workflow-manager.jar`.

---

## Git hooks

The pre-push hook at `.githooks/pre-push` runs `gradle test` before every push.
Git does **not** pick up `.githooks/` automatically — `core.hooksPath` must be configured.

```bash
gradle installGitHooks   # sets core.hooksPath=.githooks (run once after cloning)
```

The `release` task calls `git push` internally via `ProcessBuilder`, so the hook fires
automatically during a release — tests must pass before any release commit goes out.
To bypass in a genuine emergency: `git push --no-verify` (not recommended).

### WSL/NTFS gotchas for hook files

- **Never edit `.githooks/` scripts with a Windows editor or the Write tool** — they silently
  add CRLF line endings. Linux `exec()` reads the shebang as `#!/bin/sh\r`, looks for an
  interpreter named `/bin/sh` + carriage-return, and fails with `No such file or directory`.
- Always write hook scripts from WSL using a heredoc: `cat > .githooks/pre-push << 'EOF'`
- `git config core.hooksPath` also fails on NTFS (can't chmod the lock file). The
  `installGitHooks` task handles this with a direct `.git/config` file edit as a fallback.

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
| Application service / use case | Mockito — mock all ports and the repository |
| Infrastructure (file I/O) | `@TempDir` — never touch `~/.workflow-manager/` in tests |
| EDT / Swing | Not tested; keep presentation logic minimal |

**`JsonWorkflowRepository` test constructor:** The class exposes a package-private
constructor `JsonWorkflowRepository(Path configFile, Consumer<String> logger)` that
accepts an explicit file path — use this with `@TempDir` for full isolation.

```java
@TempDir Path tempDir;

JsonWorkflowRepository repo = new JsonWorkflowRepository(
        tempDir.resolve("workflows.json"), msg -> {});
```

**Mockito tip:** `WorkflowAppService` and `CheckForUpdateUseCase` take all their
dependencies as constructor-injected interfaces — mock them directly, no spy needed.

---

## Adding a new feature

1. **Define the capability** in `domain/` (if it's a new concept) or add a port interface
   in `application/` (if it's a new output boundary, e.g. notifications).
2. **Implement the port** in `infrastructure/` using the concrete library.
3. **Wire it up** in `App.java` (composition root) only — pass the implementation via
   constructor injection to `WorkflowAppService` or the relevant use case.
4. **Expose it to the UI** through `WorkflowAppService` or a new use case; the
   `presentation/` layer must never import an `infrastructure/` class.

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
| Logger | `Consumer<String>` passed via constructor; `App.java` starts with `System.out::println`, rewires to `window::log` after window creation via `AtomicReference` |
| Hotkey callback | Fired on JNativeHook thread → dispatched to a new background thread |
| Immutability | `Workflow`, `AppEntry`, `Hotkey` — final fields, `Collections.unmodifiableList` copies |
| `WorkflowRepository` | `findAll()`, `reload()`, `save(List<Workflow>)`, `reset()`, `configPath()` |

---

## WSL / NTFS reference

| Problem | Cause | Fix |
|---|---|---|
| `gradlew` not executable | NTFS has no execute bit | Use `gradle <task>` directly |
| `shadowJar` / `processResources` chmod errors | Gradle writes to NTFS | Build dir redirected to `/tmp/wm-build` |
| `git config` fails (exit 4) | Can't chmod `.git/config.lock` | Edit `.git/config` directly or use `gradle installGitHooks` |
| Hook runs but `No such file or directory` | Script has CRLF endings | Rewrite with `cat > file << 'EOF'` from WSL |
| `git push` shows chmod warning | Non-fatal NTFS noise | Safe to ignore — push still succeeds |
