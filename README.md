# WorkflowManager

A lightweight desktop utility that switches your entire working context — apps, browsers, tools — with a single hotkey or button click.

---

## Features

- **One-click workflow activation** — open a predefined set of applications and URLs
- **Global hotkeys** — trigger workflows even when the app is minimised (powered by JNativeHook)
- **Close other workflows** — optionally kill processes from all other workflows before launching
- **System tray** — runs quietly in the background; double-click the tray icon to show the window
- **Live config reload** — edit `workflows.json` and press "Reload Config" without restarting
- **Dark mode** — FlatLaf dark theme out of the box

---

## Quick Start

### 1. Build

```bash
gradle shadowJar
```

Produces: `build/libs/workflow-manager-1.0-SNAPSHOT.jar`

### 2. Run

```bash
java -jar build/libs/workflow-manager-1.0-SNAPSHOT.jar
```

> **First launch:** a default `~/.workflow-manager/workflows.json` is created automatically.
> Edit it and press "Reload Config" to apply your changes.

---

## Config reference

Config file location: `~/.workflow-manager/workflows.json`

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
          "path": "idea64.exe",  // Executable to launch (must be on PATH or absolute)
          "args": [],            // Optional extra command-line arguments
          "delayMs": 0           // Milliseconds to wait before launching this entry
        },
        {
          "name": "Docs",
          "url": "https://docs.example.com",  // Set url instead of path to open a URL
          "delayMs": 500
        }
      ],
      "close": ["slack.exe"],    // Process names to kill before opening this workflow
      "closeOthers": false       // If true, kill processes from ALL other workflows first
    }
  ]
}
```

### Notes

- Set either `"path"` **or** `"url"` in an `open` entry, not both.
- `"args"` and `"delayMs"` are optional (default: `[]` and `0`).
- `"close"` and `"closeOthers"` are optional (default: `[]` and `false`).
- `"closeOthers": true` overrides `"close"`.

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

## Architecture

See [CLAUDE.md](CLAUDE.md) for the DDD layer diagram, import rules, and developer conventions.
