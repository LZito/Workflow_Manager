# WorkflowManager — Developer Guide

## Architecture

WorkflowManager follows **Domain-Driven Design** with a strict layered architecture.

```
┌──────────────────────────────────────────────────┐
│  ui/                  (Swing)                    │
│    depends on: application only                  │
└────────────────────┬─────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────┐
│  application/         (use cases + ports)         │
│    depends on: domain only                        │
└──────┬───────────────────────────────────┬────────┘
       │                                   │
┌──────▼───────┐           ┌───────────────▼────────┐
│  domain/     │           │  infrastructure/        │
│  (pure Java) │           │  (implements ports)     │
│  no deps     │           │  Jackson, JNativeHook   │
└──────────────┘           └────────────────────────┘
```

### Layer rules (strictly enforced)

| Layer | May import | Must NOT import |
|---|---|---|
| `domain` | JDK only | Everything else |
| `application` | `domain`, JDK | `infrastructure`, `ui`, Jackson, JNativeHook |
| `infrastructure` | `application`, `domain`, JDK, 3rd-party libs | `ui` |
| `ui` | `application`, `domain`, JDK, Swing, FlatLaf | `infrastructure` |

`App.java` (composition root) is the **only** class allowed to import across all layers.

---

## Package structure

```
at.lzito.workflowmanager/
├── App.java                              ← composition root (wires everything)
│
├── domain/
│   ├── Workflow.java                     ← Aggregate Root; identity = name
│   ├── AppEntry.java                     ← Value Object (immutable)
│   ├── Hotkey.java                       ← Value Object (normalised lowercase)
│   └── WorkflowRepository.java           ← Repository interface (output port)
│
├── application/
│   ├── ActivateWorkflowUseCase.java      ← close others + launch apps
│   ├── ReloadWorkflowsUseCase.java       ← reload config + rebind hotkeys
│   └── port/
│       ├── ProcessLauncherPort.java      ← launch / kill processes
│       └── HotkeyRegistryPort.java       ← register / bind hotkeys
│
├── infrastructure/
│   ├── persistence/
│   │   └── JsonWorkflowRepository.java   ← Jackson impl; private inner DTOs
│   ├── process/
│   │   └── OsProcessLauncher.java        ← ProcessLauncherPort impl
│   └── hotkey/
│       └── JNativeHookRegistry.java      ← HotkeyRegistryPort + NativeKeyListener
│
└── ui/
    └── MainWindow.java                   ← JFrame; takes use cases in constructor
```

---

## Build commands

```bash
gradle compileJava   # compile only (fastest feedback cycle)
gradle test          # run unit tests
gradle shadowJar     # fat JAR → build/libs/workflow-manager-1.0-SNAPSHOT.jar
gradle build         # compile + test + shadowJar
```

> **WSL / NTFS note:** `gradlew` is not executable on NTFS. Run `gradle` directly from WSL,
> or use `gradlew.bat` from a Windows terminal.

---

## Adding a new feature

1. **Define the capability in `domain/` or `application/port/`** — if it's a new output
   boundary (e.g. notifications), add a port interface in `application/port/`.
2. **Implement the port** in `infrastructure/` using the concrete library.
3. **Wire it up** in `App.java` (composition root only).
4. **Expose it to the UI** through a new or extended use case, never directly.

Never let `ui/` or `application/` import infrastructure classes — pass them via constructor
injection (port interfaces only).

---

## Hotkey format

Hotkeys are stored in config as lowercase, `+`-separated strings:

```
ctrl+alt+1
ctrl+shift+f5
```

Modifier ordering (canonical): `ctrl` → `alt` → `shift` → `meta`.

`JNativeHookRegistry.buildCombo()` produces strings in this exact order so that they
match the normalised keys stored by `Hotkey.of()`.

---

## Key conventions

| Convention | Rule |
|---|---|
| Composition root | `App.java` only — no logic, just wiring |
| Jackson DTOs | Private inner classes in `JsonWorkflowRepository`; never escape to domain |
| EDT safety | Swing calls via `SwingUtilities.invokeLater`; activation on background thread |
| Logger | `Consumer<String>` passed via constructor; starts as `System.out::println`, rewired to `window::log` after the window is created |
| Hotkey callback | Fired on JNativeHook thread → `onHotkeyActivated` dispatches to a new background thread |
| Immutability | Domain objects (`Workflow`, `AppEntry`, `Hotkey`) are fully immutable |
