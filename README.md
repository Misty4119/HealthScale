# HealthScale

**Fork/build notice:** This checkout's Gradle build produces `build/libs/HealthScale-<version>.jar`. The linked GitHub Releases page contains published artifacts and may not match the current checkout or its uncommitted changes.


## Current version and tested compatibility

**Current deliverable version:** `2.5.0`.

**Target:** Minecraft 26.2–26.3 on Paper, Folia, and Canvas.

**Tested builds:** Paper 26.2 #135 and 26.3 #242; Folia 26.2 #7, 26.3 #3 (required), and #6; Canvas 26.2 #994 and 26.3 #1010 (latest successful 26.3 build checked 2026-11-06). **Verification scope:** Startup, plugin enablement, and general console smoke only; player workflows, data migrations, optional integrations, and cross-server behavior were not tested.

## Features

- **Global health-scale** — set a single display value for all worlds
- **Per-world overrides** — different scale for each world (e.g. scale 80 = 40 hearts in your RPG world, scale 20 = 10 hearts in the arena)
- **Live reload** — `/healthscale reload` reloads `config.yml` and reapplies to all online players instantly, no restart needed
- **Runtime set** — `/healthscale set <value>` changes the global scale on the fly without touching the config file
- **Folia-native** — all player mutations are dispatched through `EntityScheduler`; bulk operations through `GlobalRegionScheduler`
- **MiniMessage** — every message is fully customizable with MiniMessage formatting
- **Configurable bounds** — `min-scale` / `max-scale` guard against nonsensical values; invalid entries fall back gracefully

---

## Requirements

| Requirement | Version |
|---|---|
| Java | 25+ |
| Compile API | Canvas API pinned in `build.gradle.kts` |
| Server | Paper-compatible API; Folia/Canvas support declared in the descriptor |

> The current build targets Java 25 and pins its Canvas API in `build.gradle.kts`.

---

## Installation

1. Build this checkout and install its JAR from `build/libs/`, or choose a published artifact from [Releases](https://github.com/Misty4119/HealthScale/releases) after checking that release's source revision and compatibility notes.
2. Drop it into your server's `plugins/` folder.
3. (Re)start the server — a default `config.yml` is generated automatically.
4. Edit `plugins/HealthScale/config.yml` to your liking.
5. Run `/healthscale reload` to apply changes without restarting.

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/healthscale info` | Show the current plugin version and active scale values | `healthscale.use` |
| `/healthscale reload` | Reload `config.yml` and reapply to all online players | `healthscale.admin` |
| `/healthscale set <value>` | Change the global health-scale at runtime | `healthscale.admin` |

**Aliases:** `/hs`, `/hscale`

---

## Permissions

| Node | Default | Description |
|---|---|---|
| `healthscale.use` | everyone | Access to `/healthscale info` |
| `healthscale.admin` | OP | Access to `reload` and `set` subcommands |
| `healthscale.*` | OP | Grants all permissions above |

---

## Configuration

```yaml
# plugins/HealthScale/config.yml

# Enable or disable health scaling entirely
enabled: true

# Default display scale for all worlds (20.0 = 10 hearts)
# This is a display-health value: two scale points represent one heart; actual max HP is unchanged
health-scale: 20.0

# Allowed range for health-scale values
min-scale: 2.0
max-scale: 2048.0

# Per-world overrides — worlds listed here use their own scale value
# Worlds not listed fall back to health-scale above
world-overrides:
  # rpg:world:
  #   health-scale: 80.0   # 40 hearts for your RPG world
  # world_nether:
  #   health-scale: 20.0   # standard 10 hearts in the arena

# All messages support MiniMessage formatting
# Available placeholders are shown in the comments below
messages:
  no-permission: "<red>You do not have permission to execute this command!</red>"
  reload-start: "<yellow>Reloading HealthScale configuration...</yellow>"
  reload-success: "<green>Configuration reloaded successfully! Global health scale: <white><scale></white></green>"
  reload-invalid-scale: "<red>Invalid health-scale value in config.yml (must be between <min> and <max>). Restored to default.</red>"
  set-success: "<green>Health display scale set to <white><scale></white>.</green>"
  set-invalid: "<red>Invalid value. Please specify a number between <min> and <max>.</red>"
  usage: "<red>Usage: <white>/healthscale <reload|set <value>|info></white></red>"
  info: "<aqua>HealthScale v<version> | Global scale: <white><scale></white> | World overrides: <white><overrides></white></aqua>"
  plugin-enabled: "<green>HealthScale enabled successfully.</green>"
  plugin-disabled: "<yellow>HealthScale disabled. Reset health scale for all players.</yellow>"
```

### Scale value reference

| `health-scale` | Hearts displayed |
|---|---|
| `20.0` | 10 hearts (vanilla default) |
| `40.0` | 20 hearts |
| `80.0` | 40 hearts |
| `200.0` | 100 hearts |

The player's actual max HP is not affected — only the visual representation changes.

---

## How it works

Minecraft's `Player#setHealthScaled` and `Player#setHealthScale` control how the client renders the heart bar.
When set to, say, `80.0`, a player with 2000 max HP will still see a full bar of 40 hearts — their HP is just mapped to that display range.

HealthScale hooks into `PlayerJoinEvent`, `PlayerRespawnEvent`, and `PlayerChangedWorldEvent` to (re)apply the correct scale at every relevant moment.
On Folia, every state mutation is dispatched to the owning region thread via `EntityScheduler` to stay thread-safe.

---

## Building from source

```bash
git clone https://github.com/Misty4119/HealthScale.git
cd healthscale
./gradlew shadowJar
```

The output jar is at `build/libs/HealthScale-2.5.0.jar`.

---

## License

This project is open source. See [LICENSE](LICENSE) for details.

---

## Author

Made by **Misty4119** ([Misty4119](https://github.com/Misty4119))
