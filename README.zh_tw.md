# HealthScale

**Fork／建置說明：**此 checkout 的 Gradle build 會產生 `build/libs/HealthScale-<version>.jar`。連結的 GitHub Releases 頁面提供已發布的產物，其內容可能與目前 checkout 或其中未提交的變更不同。


## 目前版本與實測相容性

**目前輸出版本：** `2.4.0`。

**目標：** Minecraft 26.2–26.3，使用 Paper、Folia 與 Canvas。

**已測試建置：** Paper 26.2 #135 與 26.3 #241；Folia 26.2 #7、26.3 #3（指定 build）與 #6；Canvas 26.2 #994 與 26.3 #1009（2026-11-05 查得最新成功的 26.3 build）。**驗證範圍：**僅啟動、外掛載入與一般主控台 smoke test；未測試玩家流程、資料遷移、選用整合或跨伺服器行為。

## 功能

- **全域生命值比例** — 為所有世界設定同一個顯示值。
- **各世界覆寫值** — 為不同世界設定不同比例（例如比例 80 = RPG 世界顯示 40 顆心，比例 20 = 競技場顯示 10 顆心）。
- **即時重新載入** — `/healthscale reload` 會重新載入 `config.yml`，並立即套用到所有線上玩家，不需重新啟動。
- **執行期間設定** — `/healthscale set <value>` 可即時變更全域比例，不會修改設定檔。
- **原生支援 Folia** — 所有玩家狀態變更都透過 `EntityScheduler` 派送；批次操作則透過 `GlobalRegionScheduler`。
- **MiniMessage** — 所有訊息都可使用 MiniMessage 格式完整自訂。
- **可設定範圍** — `min-scale`／`max-scale` 可防止不合理的值；無效設定會妥善回復預設值。

---

## 需求

| 需求 | 版本 |
|---|---|
| Java | 25+ |
| 編譯 API | `build.gradle.kts` 中固定的 Canvas API 版本 |
| 伺服器 | 相容 Paper 的 API；描述檔宣告支援 Folia／Canvas |

> 目前 build 以 Java 25 為目標，並在 `build.gradle.kts` 中固定 Canvas API 版本。

---

## 安裝

1. 建置此 checkout 並使用 `build/libs/` 中的 JAR；或從 [Releases](https://github.com/Misty4119/HealthScale/releases) 選擇已發布的產物，並先確認該 release 的原始碼 revision 與相容性說明。
2. 將 JAR 放入伺服器的 `plugins/` 資料夾。
3. 啟動或重新啟動伺服器；系統會自動產生預設的 `config.yml`。
4. 依需求編輯 `plugins/HealthScale/config.yml`。
5. 執行 `/healthscale reload`，不需重新啟動即可套用變更。

---

## 指令

| 指令 | 說明 | 權限 |
|---|---|---|
| `/healthscale info` | 顯示目前外掛版本與啟用中的比例值 | `healthscale.use` |
| `/healthscale reload` | 重新載入 `config.yml` 並套用到所有線上玩家 | `healthscale.admin` |
| `/healthscale set <value>` | 在執行期間變更全域生命值比例 | `healthscale.admin` |

**別名：** `/hs`、`/hscale`

---

## 權限

| 權限節點 | 預設 | 說明 |
|---|---|---|
| `healthscale.use` | 所有玩家 | 可使用 `/healthscale info` |
| `healthscale.admin` | OP | 可使用 `reload` 與 `set` 子指令 |
| `healthscale.*` | OP | 授予上述所有權限 |

---

## 設定

```yaml
# plugins/HealthScale/config.yml

# 完全啟用或停用生命值比例功能
enabled: true

# 所有世界的預設顯示比例（20.0 = 10 顆心）
# 這是生命值顯示值：每 2 點代表 1 顆心；實際最大 HP 不變
health-scale: 20.0

# health-scale 的允許範圍
min-scale: 2.0
max-scale: 2048.0

# 各世界的覆寫值 — 列在此處的世界會使用自己的比例
# 未列出的世界會使用上方的 health-scale
world-overrides:
  # rpg:world:
  #   health-scale: 80.0   # RPG 世界顯示 40 顆心
  # world_nether:
  #   health-scale: 20.0   # 競技場標準顯示 10 顆心

# 所有訊息都支援 MiniMessage 格式
# 可用的 placeholder 顯示於下方註解
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

### 比例值參考

| `health-scale` | 顯示的心數 |
|---|---|
| `20.0` | 10 顆心（原版預設值） |
| `40.0` | 20 顆心 |
| `80.0` | 40 顆心 |
| `200.0` | 100 顆心 |

玩家實際的最大 HP 不受影響；變更的只有視覺呈現。

---

## 運作方式

Minecraft 的 `Player#setHealthScaled` 與 `Player#setHealthScale` 控制用戶端如何呈現愛心列。
例如設定為 `80.0` 時，即使玩家的最大 HP 是 2000，仍會看到顯示 40 顆心的完整血條 — 只是將 HP 映射到這個顯示範圍。

HealthScale 會監聽 `PlayerJoinEvent`、`PlayerRespawnEvent` 與 `PlayerChangedWorldEvent`，並在相關時機重新套用正確的比例。
在 Folia 上，所有狀態變更都會透過 `EntityScheduler` 派送到所屬的 region 執行緒，以確保執行緒安全。

---

## 從原始碼建置

```bash
git clone https://github.com/Misty4119/HealthScale.git
cd healthscale
./gradlew shadowJar
```

輸出 JAR 位於 `build/libs/HealthScale-2.4.0.jar`。

---

## 授權

此專案為開放原始碼，詳情請見 [LICENSE](LICENSE)。

---

## 作者

由 **Misty4119** 製作（[Misty4119](https://github.com/Misty4119)）。
