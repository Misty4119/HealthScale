# HealthScale

A lightweight Canvas/Folia/Paper plugin that scales the health display in players' HUD — no matter how many actual hit points a player has, the heart bar always renders at the configured value.
Designed for high-health RPG servers and compatible with Canvas/Folia multi-threaded region scheduling.

---

## Features

- **Global health-scale** — set a single display value for all worlds
- **Per-world overrides** — different scale for each world (e.g. 80 hearts in your RPG world, 20 in the arena)
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
| Canvas | 26.2 build 923 stable |
| Minecraft | 26.2 |

> The plugin is compiled against Canvas API `26.2.build.923-stable` and declares both `folia-supported: true` and `canvas-supported: true`.

---

## Installation

1. Download the latest `HealthScale-x.x.x.jar` from [Releases](https://github.com/Misty4119/HealthScale/releases).
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
# The HUD always shows this many hearts regardless of the player's actual max HP
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

The output jar is at `build/libs/HealthScale-2.1.1.jar`.

---

## License

This project is open source. See [LICENSE](LICENSE) for details.

---

## Author

Made by **Misty4119** ([Misty4119](https://github.com/Misty4119))
