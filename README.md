# HealthScale

A lightweight Paper/Folia plugin that scales the health display in players' HUD — no matter how many actual hit points a player has, the heart bar always renders at the configured value.
Designed for high-health RPG servers and fully compatible with Folia's multi-threaded region scheduling.

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
| Paper / Folia | 1.21 (API), tested on 26.1.2 |
| Minecraft | 1.21+ |

> The plugin is compiled against the Paper API and declares `folia-supported: true`, so it runs on both Paper and Folia.

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
  # rpg_world:
  #   health-scale: 80.0   # 40 hearts for your RPG world
  # arena_world:
  #   health-scale: 20.0   # standard 10 hearts in the arena

# All messages support MiniMessage formatting
# Available placeholders are shown in the comments below
messages:
  no-permission: "<red>你沒有權限執行此指令！</red>"
  reload-start: "<yellow>正在重載 HealthScale 設定...</yellow>"
  reload-success: "<green>設定重載完成！全域血量縮放: <white><scale></white></green>"   # {scale}
  reload-invalid-scale: "<red>health-scale 數值無效（必須介於 <min> ~ <max>），已還原預設值。</red>"  # {min} {max}
  set-success: "<green>已將血量顯示縮放設為 <white><scale></white> 顆心。</green>"   # {scale}
  set-invalid: "<red>無效數值。請輸入介於 <min> ~ <max> 之間的數字。</red>"   # {min} {max}
  usage: "<red>用法：<white>/healthscale <reload|set <數值>|info></white></red>"
  info: "<aqua>HealthScale v<version> | 全域縮放: <white><scale></white> | 世界覆蓋: <white><overrides></white> 個</aqua>"  # {version} {scale} {overrides}
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

The output jar is at `build/libs/HealthScale-2.0.2.jar`.

---

## License

This project is open source. See [LICENSE](LICENSE) for details.

---

## Author

Made by **Misty4119** ([Misty4119](https://github.com/Misty4119))
