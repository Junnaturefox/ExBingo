# ExBingo

<details open>
<summary>中文说明</summary>

## 简介

ExBingo 是 [Yet Another Bingo](https://modrinth.com/mod/yet-another-minecraft-bingo) 的 **NeoForge 1.21.1** 移植版本，提供多人 Bingo 物品收集玩法。

移植过程中对原模组做了大量重写与调整，因此部分行为与原模组不同。**当前版本只附带 Minecraft 原版内容的棋盘**：不包含任何其他模组的专属棋盘、难度表或目标定义。

| 项目 | 值 |
|---|---|
| Minecraft | 1.21.1 |
| 加载器 | NeoForge 21.1.234 |
| 许可证 | LGPL-3.0-only |
| 运行环境要求 | Java 21 |

## 玩法与内置功能

- **多人 Bingo 对局**：队伍、棋盘、大厅、倒计时、结算的完整流程。
- **棋盘（Board）**：见下方「内置棋盘」。
- **队伍共享箱**：每个 Bingo 队伍拥有持久化共享箱，可配置是否参与 Bingo 物品目标判定、是否在消耗物品模式下消耗。对局重置时清空。
- **队伍传送**：同队玩家可互相传送。
- **旁观者申请入队**：对局进行中，旁观者可打开队伍列表向某一队发起入队申请，队内玩家点同意即可加入（无需管理员干预）。
- **客户端 HUD**：安装本模组后可在客户端显示 Bingo 卡片、消息与队伍进度。
- **自动分级（autotier）**：按获取难度为未分级内容自动生成难度表，不会改动原版内容或已分配的内容。
- **对局统计**：对局历史、最佳用时、连胜等，默认存于 `config/exbingo/bingo.db`（SQLite，可用 MySQL/PostgreSQL 等外部数据库）。
- **长对局性能维护**：实体数量超过阈值后，定期清理远离玩家的掉落物、投射物与普通生物，保留玩家、坐骑、命名/持久/无敌实体与受保护命名空间。

### 内置棋盘

| 棋盘 ID | 名称 | 说明 |
|---|---|---|
| `items` | Items | 物品收集，通用 |
| `in_overworld` | Overworld | 主世界可获得的内容 |
| `advancements` | Advancements | 成就类目标 |
| `simplified` | Simplified | 精简目标集 |

此外还有若干**过滤器**（不是独立棋盘）：`everything`（全部）、`uncategorized`（未分类）、`unobtainable`（不可获取）等，以及 `in_the_nether` / `in_the_end` 等标签集合，可在 `/bingo filter` 中使用。

OP 可以关闭不想用的棋盘：

- `/bingo carddisable <棋盘>` — 关闭棋盘；关闭后不会出现在选择界面，也不会被「全选」选入。
- `/bingo carddisable list` — 列出已关闭的棋盘。
- `/bingo cardenable <棋盘>` — 重新启用。

关闭列表保存在 `config/exbingo/config.json` 的 `disabledFilterPresets` 字段，立即生效、重启保留。

## 关于其他模组的内容

**本模组不再附带任何其他模组的联动棋盘。** 其他模组加入的物品/成就默认**不会**进入棋盘，行为由两个配置控制：

- `excludeModUncategorizedFromCards`（默认 `true`）——未分类的模组内容默认排除；原版（`minecraft:`）与模组自带内容不受影响。
- `excludeUnbreakableBlocksFromCards`（默认 `true`）——不可破坏方块（基岩、屏障、命令方块等）默认排除。

需要时可以用过滤器显式选入（例如 `+from=<模组id>`、`+uncategorized`），或用 `/bingo autotier generate` 为未分类内容自动分级后再启用。

## 保留的模组联动（可选，缺失时自动降级）

以下联动代码保留，但都只在对应模组存在时生效，缺失不会报错：

| 模组 | 作用 | 声明方式 |
|---|---|---|
| JEI | 从棋盘目标直接打开物品配方 | `neoforge.mods.toml` 可选依赖（客户端） |
| Simple Voice Chat | 按 Bingo 队伍管理语音分组，可在 `voicechat.json` 关闭 | `neoforge.mods.toml` 可选依赖 |
| Xaero 的小地图 / 世界地图 | 对局中同步同队远距离玩家位置；回到大厅时清理本局地图缓存与路径点 | 运行时软检测 |
| Curios | 对局重置时清空饰品栏 | 运行时软检测 |
| Chunky | 预生成区块 | 运行时软检测 |
| 权限 / Vanish / 占位符 | OP 判定、隐身兼容、占位符解析 | 运行时软检测 |

## 指令

所有指令使用权限节点（如 `bingo.command.reset`），默认值为 `ALLOW` / `OPERATORS` / `DENY` 三档：`OPERATORS` 表示需要 OP 权限等级 2，单人存档视为满足。安装权限管理模组时可覆盖这些默认值。

| 指令 | 说明 |
|---|---|
| `/bingo start`、`/bingo start ignore_warnings` | 开始对局（`ignore_warnings` 跳过数据包推荐人数等警告） |
| `/bingo end` | 结束当前对局（仅对局进行中可用） |
| `/bingo reset` | 重置对局与大厅状态（对局中或结算后可用） |
| `/bingo goal <数量> <items\|lines>`、`/bingo goal full_card` | 胜利目标 |
| `/bingo timelimit <分钟>` | 对局时限 |
| `/bingo options mode <lockout\|inventory\|hidden_items\|consume_items>` | 对局模式 |
| `/bingo options stalemate <end_game\|reroll_card\|do_nothing>` | 僵局处理 |
| `/bingo options end_when <never\|first_win\|teams_win\|all_win>` | 结束条件 |
| `/bingo options pvp [enabled]`、`elytra [enabled]`、`night_vision [enabled]` | 开关类规则 |
| `/bingo options spawn_distance <距离>`、`spawn_dimension <维度>` | 出生点设置 |
| `/bingo options play_to <分数>`、`play_to cards <数量>`、`play_to infinite_cards`、`play_to replace_goals` | 卡牌数量与进度规则 |
| `/bingo options preview_card` | 预览棋盘 |
| `/bingo card`、`/bingo cards`、`/bingo card <id>` | 查看棋盘 |
| `/bingo carddisable` / `/bingo cardenable` | 关闭 / 启用棋盘 |
| `/bingo filter [表达式]` | 按标签/条件过滤目标 |
| `/bingo autotier generate` / `clear` | 为未分类内容自动分级 |
| `/bingo debug` | 输出调试信息到服务端日志 |
| `/bingo`（不带参数） | 显示当前对局选项摘要 |
| `/bingodata tag add\|remove`、`/bingodata tierlist add\|remove` | 就地编辑标签与难度表（支持通配符批量操作） |
| `/bingostats` | 查看统计数据 |
| `/bingoprefs` | 玩家个人偏好（HUD、计分板、提示消息等） |
| `/join` | 玩家加入 / 切换队伍 |
| `/ready` | 开始前准备确认 |
| `/spectator` | 切换旁观者模式 |
| `/teamchest`、`/tc` | 打开队伍共享箱 |
| `/teamtp <玩家>`、`/ttp <玩家>` | 传送到同队玩家 |
| `/tptoggle`、`/teamchest toggle`、`/teamchest count` | OP 在对局开始前切换相关功能 |
| `/coords` | 同队共享坐标 |

## 按键

| 功能 | 默认键 |
|---|---|
| 查看 Bingo 卡片 | `Y` |
| 打开队伍共享箱 | `B` |
| 显示 / 隐藏 HUD | 未绑定（可在「控制」中设置） |

## 配置文件

服务端配置位于 `config/exbingo/`，可直接用文本编辑，改动后 `/reload` 或重启生效：

- `config.json` — 主配置（棋盘权重、队伍功能、统计、自动分级参数、权限、已关闭棋盘等）。
- `game-options.json` — 对局与棋盘选项（目标、分布、模式、出生点、结束条件）。
- `tierlists/*.tierlist.json` — 难度表（S/A/B/C/D），可自行增删以定义自定义棋盘。
- `tierlists/autotier.tierlist.json` — 由 `/bingo autotier generate` 生成。
- `voicechat.json` — 语音分组设置（`enabled` 可整体关闭）。
- `files.json` — 本模组对上述文件的追踪记录，用于判断文件是否被用户修改过。
- `commands/*.mcfunction` — 状态切换时执行的指令脚本（如 `on_countdown`）。

NeoForge 侧还提供两份 TOML 配置（`config/exbingo-common.toml`、`config/exbingo-client.toml`），与 `config.json` 双向同步：**JSON 是运行时权威来源**，TOML 仅镜像可映射的字段并在数值变化时回写。

## 构建与开发

```bash
./gradlew build          # 产物：build/libs/exbingo-<version>.jar
./gradlew test           # 单元测试
./gradlew runClient      # 开发客户端
./gradlew runServer      # 开发服务端（首次需 run/eula.txt）
./gradlew downloadAssets # 预下载客户端资源
```

需要 **JDK 21**。首次构建会由 ModDevGradle 执行 NeoForm 反编译，耗时较长。

持续集成：仓库带 GitHub Actions 工作流（`.github/workflows/build-and-release.yml`）。推送 `main` 只构建；推送形如 `v1.0.9` 的 tag 会校验 tag 与 `gradle.properties` 中的 `mod_version` 一致、构建并发布 GitHub Release（自动附带 jar 与 CHANGELOG 对应版本段落）；也可手动触发只构建。

## 许可证与来源

ExBingo 是 Yet Another Bingo 的派生版本，以 `LGPL-3.0-only` 分发。队伍共享箱与队伍传送功能参考了 MIT 许可的 [YetAnotherBingo-TeamChest](https://github.com/ImCZFy/YetAnotherBingo-TeamChest)（Copyright (c) 2025 ChengZhiFy）。许可证文本见 [LICENSE](LICENSE) 与 [COPYING](COPYING)，来源与鸣谢见 [NOTICE](NOTICE)，版本变更见 [CHANGELOG.md](CHANGELOG.md)。

</details>

<details>
<summary>English</summary>

## Introduction

ExBingo is a **NeoForge 1.21.1** port of [Yet Another Bingo](https://modrinth.com/mod/yet-another-minecraft-bingo), providing a multiplayer Bingo item-hunt game mode.

The port rewrote and adjusted a large part of the upstream mod, so some behavior differs from the original. **This version ships only vanilla Minecraft content**: no per-mod boards, tier lists, or objective definitions are bundled.

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.234 |
| License | LGPL-3.0-only |
| Runtime | Java 21 |

## Features

- **Multiplayer Bingo rounds**: teams, boards, lobby, countdown, and results flow.
- **Boards**: see "Built-in boards" below.
- **Team chest**: every team gets a persistent shared chest, configurable for Bingo item scoring and consume-items mode. Cleared when the game resets.
- **Team teleport**: teammates can teleport to each other.
- **Spectator team join**: during a round a spectator can open the team list and request to join a team; any player on that team can approve it, no operator needed.
- **Client HUD**: optional client-side Bingo card, messages, and team progress display.
- **Auto tiering**: generates a tier list for uncategorized content by obtain difficulty. It never changes vanilla content or anything already assigned.
- **Statistics**: game history, best times, win streaks. Stored in `config/exbingo/bingo.db` by default (SQLite; external databases are supported).
- **Long-game performance maintenance**: once the entity count passes a threshold, distant drops, projectiles, and ordinary mobs are cleaned up periodically; players, mounts, named/persistent/invulnerable entities, and protected namespaces are kept.

### Built-in boards

| Board ID | Name | Notes |
|---|---|---|
| `items` | Items | General item collection |
| `in_overworld` | Overworld | Content obtainable in the overworld |
| `advancements` | Advancements | Advancement objectives |
| `simplified` | Simplified | A reduced objective set |

There are also **filters** (not separate boards) such as `everything`, `uncategorized`, `unobtainable`, plus tag sets like `in_the_nether` / `in_the_end` for `/bingo filter`.

Operators can disable boards they do not want:

- `/bingo carddisable <board>` — disable a board; it never appears in the board selection and is never included by "select all".
- `/bingo carddisable list` — list disabled boards.
- `/bingo cardenable <board>` — re-enable a board.

The disabled set lives in `disabledFilterPresets` in `config/exbingo/config.json`, applies immediately, and survives restarts.

## Other mods' content

**This mod no longer bundles boards for other mods.** Items and advancements from other mods are kept off cards by default; two settings control this:

- `excludeModUncategorizedFromCards` (default `true`) — uncategorized modded content is excluded; vanilla (`minecraft:`) and mod-provided categories are unaffected.
- `excludeUnbreakableBlocksFromCards` (default `true`) — unbreakable blocks (bedrock, barriers, command blocks, ...) are excluded.

You can still opt content in through filters (e.g. `+from=<modid>`, `+uncategorized`), or run `/bingo autotier generate` to classify uncategorized content first.

## Optional mod integrations (kept, degrade gracefully)

All of these only activate when the respective mod is present; nothing breaks when it is missing.

| Mod | Purpose | Declared as |
|---|---|---|
| JEI | Open item recipes from a card target | optional dependency in `neoforge.mods.toml` (client) |
| Simple Voice Chat | Manage voice groups per Bingo team; can be turned off in `voicechat.json` | optional dependency in `neoforge.mods.toml` |
| Xaero's Minimap / World Map | Sync distant teammates during a round; clear the round's map cache and waypoints when returning to the lobby | runtime soft detection |
| Curios | Clear accessory slots on game reset | runtime soft detection |
| Chunky | Chunk pre-generation | runtime soft detection |
| Permissions / Vanish / Placeholders | Operator checks, invisibility compatibility, placeholder resolution | runtime soft detection |

## Commands

Commands use permission nodes (e.g. `bingo.command.reset`) whose defaults are `ALLOW` / `OPERATORS` / `DENY`: `OPERATORS` means op permission level 2, and singleplayer counts as op. Installing a permissions mod lets you override those defaults.

| Command | Description |
|---|---|
| `/bingo start`, `/bingo start ignore_warnings` | Start a round (`ignore_warnings` skips datapack player-count warnings) |
| `/bingo end` | End the current round (only while a round is running) |
| `/bingo reset` | Reset the round and lobby state (while playing or after the results) |
| `/bingo goal <count> <items\|lines>`, `/bingo goal full_card` | Win goal |
| `/bingo timelimit <minutes>` | Round time limit |
| `/bingo options mode <lockout\|inventory\|hidden_items\|consume_items>` | Game mode |
| `/bingo options stalemate <end_game\|reroll_card\|do_nothing>` | Stalemate behavior |
| `/bingo options end_when <never\|first_win\|teams_win\|all_win>` | End conditions |
| `/bingo options pvp [enabled]`, `elytra [enabled]`, `night_vision [enabled]` | Toggle rules |
| `/bingo options spawn_distance <distance>`, `spawn_dimension <dimension>` | Spawn settings |
| `/bingo options play_to <score>`, `play_to cards <count>`, `play_to infinite_cards`, `play_to replace_goals` | Card count and progress rules |
| `/bingo options preview_card` | Preview the card |
| `/bingo card`, `/bingo cards`, `/bingo card <id>` | Inspect the card |
| `/bingo carddisable` / `/bingo cardenable` | Disable / enable a board |
| `/bingo filter [expression]` | Filter objectives by tags/conditions |
| `/bingo autotier generate` / `clear` | Auto-tier uncategorized content |
| `/bingo debug` | Write debug info to the server log |
| `/bingo` (no arguments) | Print the current round options |
| `/bingodata tag add\|remove`, `/bingodata tierlist add\|remove` | Edit tags and tier lists in place (wildcards supported) |
| `/bingostats` | Show statistics |
| `/bingoprefs` | Per-player preferences (HUD, scoreboard, notifications, ...) |
| `/join` | Join / switch team |
| `/ready` | Ready check before the round starts |
| `/spectator` | Toggle spectator mode |
| `/teamchest`, `/tc` | Open the team chest |
| `/teamtp <player>`, `/ttp <player>` | Teleport to a teammate |
| `/tptoggle`, `/teamchest toggle`, `/teamchest count` | Operators toggle these features before a round starts |
| `/coords` | Share coordinates with your team |

## Keybinds

| Action | Default key |
|---|---|
| View Bingo Card | `Y` |
| Open Team Chest | `B` |
| Toggle Bingo HUD | unbound (set it in Controls) |

## Configuration

Server configuration lives in `config/exbingo/` and can be edited directly; run `/reload` or restart to apply:

- `config.json` — main config (board weights, team features, stats, auto-tiering, permissions, disabled boards, ...).
- `game-options.json` — round and card options (goal, distribution, modes, spawn, end conditions).
- `tierlists/*.tierlist.json` — tier lists (S/A/B/C/D); add your own to define custom boards.
- `tierlists/autotier.tierlist.json` — generated by `/bingo autotier generate`.
- `voicechat.json` — voice group settings (`enabled` turns the integration off entirely).
- `files.json` — tracking records used to tell whether a file was edited by the user.
- `commands/*.mcfunction` — command scripts run on state changes (e.g. `on_countdown`).

Two NeoForge TOML configs (`config/exbingo-common.toml`, `config/exbingo-client.toml`) mirror the mappable fields and sync both ways: **the JSON file is the runtime source of truth**, and TOML is rewritten only when a mapped value actually changes.

## Building and development

```bash
./gradlew build          # output: build/libs/exbingo-<version>.jar
./gradlew test           # unit tests
./gradlew runClient      # development client
./gradlew runServer      # development server (run/eula.txt on first launch)
./gradlew downloadAssets # pre-download client assets
```

Requires **JDK 21**. The first build runs the ModDevGradle/NeoForm decompile pipeline and takes a while.

CI: the repo ships a GitHub Actions workflow (`.github/workflows/build-and-release.yml`). Pushes to `main` build only; pushing a `v1.0.9`-style tag verifies the tag against `mod_version` in `gradle.properties`, builds, and publishes a GitHub Release with the jar and the matching CHANGELOG section. Manual dispatch builds without releasing.

## License and attribution

ExBingo is a derivative of Yet Another Bingo and is distributed under `LGPL-3.0-only`. The team chest and team teleport features reference the MIT-licensed [YetAnotherBingo-TeamChest](https://github.com/ImCZFy/YetAnotherBingo-TeamChest) project (Copyright (c) 2025 ChengZhiFy). See [LICENSE](LICENSE) and [COPYING](COPYING) for license texts, [NOTICE](NOTICE) for sources and credits, and [CHANGELOG.md](CHANGELOG.md) for release history.

</details>
