# WorkflowManager

A lightweight desktop utility that switches your entire working context — apps, browsers, tools — with a single hotkey or button click.

---

## Features

- **One-click workflow activation** — open a predefined set of applications and URLs
- **Global hotkeys** — trigger workflows even when the app is minimised (powered by JNativeHook)
- **Close other workflows** — optionally kill processes from all other workflows before launching
- **GUI config editor** — built-in editor (Edit Config button); no need to hand-edit JSON
- **System tray** — runs quietly in the background; double-click tray icon or use Show to restore; tray notification appears on hotkey-activated switches
- **Auto-update** — checks GitHub releases 4 s after startup and prompts to download and restart
- **First-run setup** — config editor opens automatically on first launch
- **Live config reload** — press Reload to pick up any external edits to `workflows.json`
- **Dark mode** — FlatLaf dark theme out of the box

---

## Quick Start

### Option A — Download (no build needed)

Download `workflow-manager.jar` from the [latest release](https://github.com/LZito/Workflow_Manager/releases/latest) and run:

```bash
java -jar workflow-manager.jar
```

### Option B — Build from source

```bash
gradle shadowJar
java -jar workflow-manager.jar   # produced in build/libs/ (or /tmp/wm-build/libs/ on WSL)
```

> **First launch:** the config editor opens automatically. Add your first workflow there,
> or close it — a default `~/.workflow-manager/workflows.json` is created and you can
> press Reload to apply manual edits at any time.

---

## Config reference

Config file: `~/.workflow-manager/workflows.json`

```jsonc
{
  "workflows": [
    {
      "name": "Coding",          // Display name (required)
      "icon": "💻",              // Optional emoji shown in the button label
      "hotkey": "ctrl+alt+1",    // Optional global hotkey (see format below)
      "open": [
        {
          "name": "IntelliJ",    // Label used in log output
          "path": "idea64.exe",  // Executable to launch (on PATH or absolute path)
          "args": [],            // Optional command-line arguments
          "delayMs": 0           // Milliseconds to wait before launching this entry
        },
        {
          "name": "Docs",
          "url": "https://docs.example.com",  // Set url instead of path to open a URL
          "delayMs": 500
        }
      ],
      "close": ["slack.exe"],    // Process names to kill when activating this workflow
      "closeOthers": false       // If true, kill ALL other workflows' processes instead
    }
  ]
}
```

### Notes

- Set either `"path"` **or** `"url"` in an `open` entry, not both.
- `"args"` and `"delayMs"` are optional (defaults: `[]` and `0`).
- `"close"` and `"closeOthers"` are optional (defaults: `[]` and `false`).
- When `"closeOthers": true`, every process listed in every *other* workflow's `"close"`
  field is killed. The activating workflow's own `"close"` list is **not** used in this mode.
- You can edit `workflows.json` by hand or use the built-in **Edit Config** dialog.

---

## Hotkey format

```
ctrl+alt+1
ctrl+shift+f5
meta+space
```

Modifier ordering: `ctrl` → `alt` → `shift` → `meta` (Windows key / Cmd).
Key names are lowercase and match Java's `NativeKeyEvent.getKeyText()` output.

---

## Auto-update

WorkflowManager checks the GitHub Releases API 4 seconds after startup. If a newer
version is found, a dialog prompts you to download and restart. The new JAR is downloaded
next to the running one, a small platform script moves it into place after exit, and the
app relaunches automatically.

The update check is configured by `src/main/resources/app.properties`:

```properties
github.owner=LZito
github.repo=Workflow_Manager
```

Set `github.owner=` (blank) to disable the update check entirely.

---

## Architecture

See [CLAUDE.md](CLAUDE.md) for the DDD layer diagram, import rules, and developer conventions.
