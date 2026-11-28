# ExBingo

<details open>
<summary>中文说明</summary>

## 简介

ExBingo 是 [Yet Another Bingo](https://modrinth.com/mod/yet-another-minecraft-bingo) 的 **NeoForge 1.21.1** 移植版本，提供多人 Bingo 物品收集玩法。移植过程中对原模组做了大量重写与调整，因此部分行为与原模组不同。

**本模组只包含 Minecraft 原版内容**：不附带任何其他模组的专属棋盘、难度表或目标定义，也不包含针对特定模组的代码补丁。

| 项目 | 值 |
|---|---|
| Minecraft | 1.21.1 |
| 加载器 | NeoForge 21.1.234 |
| 许可证 | LGPL-3.0-only |
| 运行环境 | Java 21 |

## 玩法与内置功能

- **多人 Bingo 对局**：队伍、棋盘、大厅、倒计时、结算的完整流程。
- **队伍共享箱**：每个队伍拥有持久化共享箱，可配置是否参与 Bingo 物品目标判定、是否在消耗物品模式下消耗。对局重置时清空。
- **队伍传送**：同队玩家可互相传送。
- **旁观者申请入队**：对局进行中，旁观者可打开队伍列表向某队发起入队申请，队内玩家点同意即可加入。
- **客户端 HUD**：安装本模组后可在客户端显示 Bingo 卡片、消息与队伍进度。
- **自动分级（autotier）**：按获取难度为未分类内容生成难度表，不改动原版内容或已分配的内容。
- **对局统计**：历史对局、最佳用时、连胜等，默认存于 `config/exbingo/bingo.db`（SQLite，可换外部数据库）。
- **赛后备战与投票**：大厅模式下结算界面可「准备下一轮」或直接开始下一轮；备战倒计时期间可投票选择**下一轮**或**返回大厅**，倒计时结束时按多数票执行。
- **长对局性能维护**：实体数超过阈值后定期清理远离玩家的掉落物、投射物与普通生物，保留玩家、坐骑、命名/持久/无敌实体与受保护命名空间。

### 内置棋盘

| 棋盘 ID | 名称 | 说明 |
|---|---|---|
| `items` | Items | 物品收集，通用 |
| `advancements` | Advancements | 进度类目标 |
| `simplified` | Simplified | 精简目标集 |
| `challenge` | Challenge | 挑战类目标集（成组收集等） |

此外还有若干**过滤器**（不是独立棋盘）：`everything`、`uncategorized`、`unobtainable` 等，以及 `in_the_nether` / `in_the_end` 等标签集合，可在 `/bingo filter` 中使用。

OP 可以关闭不想用的棋盘：

- `/bingo carddisable <棋盘>` — 关闭棋盘（不再出现在选择界面，也不会被「全选」选入）
- `/bingo carddisable list` — 列出已关闭的棋盘
- `/bingo cardenable <棋盘>` — 重新启用

关闭列表保存在 `config/exbingo/config.json` 的 `disabledFilterPresets`，立即生效、重启保留。

## 关于其他模组的内容

其他模组加入的物品/进度**默认不会进入棋盘**，由两个配置控制（`config/exbingo/config.json`）：

| 配置 | 默认 | 作用 |
|---|---|---|
| `excludeModUncategorizedFromCards` | `true` | 未分类的模组内容排除；原版与模组自带分类不受影响 |
| `excludeUnbreakableBlocksFromCards` | `true` | 不可破坏方块（基岩、屏障、命令方块等）排除 |

需要时可以显式选入：

- 按命名空间粗粒度引入：`/bingo filter +from=<模组id> -uncategorized -unobtainable -unbreakable`
- 精确指定每个目标：写一份 `tierlists/<名字>.tierlist.json` 并在 `itemFilterPresets` 里加一行 —— 见 [模组内容教程](模组内容教程/README.md)
- 懒得分类：`/bingo autotier generate` 自动分级后再启用

## 与外部模组的联动

本模组只与下列 4 个模组联动，**全部可选，缺失时自动降级，不影响启动与运行**：

| 模组 | 作用 | 声明方式 | 缺失时 |
|---|---|---|---|
| **JEI** | 从棋盘目标直接打开物品配方界面 | `neoforge.mods.toml` 可选依赖（仅客户端） | 无配方入口 |
| **Simple Voice Chat** | 按 Bingo 队伍管理语音分组，胜者转入旁观组，结算清理 | `neoforge.mods.toml` 可选依赖 | 不分组 |
| **Xaero's Minimap** | 对局中同步同队远距离玩家位置；回大厅清理本局地图缓存与路径点 | 运行时软检测（`isModLoaded("xaerominimap")`） | 不同步 |
| **Curios** | 对局重置时清空饰品栏 | NeoForge 能力查询 `curios:item_handler`（不引用 Curios 任何类） | 能力为空，方法直接 no-op |

其他说明：

- **权限**不依赖外部权限模组：使用内置权限节点（如 `exbingo.command.reset`），默认值 `ALLOW` / `OPERATORS` / `DENY`，其中 `OPERATORS` = OP 权限等级 2（单人存档视为满足）。
- 除上表外没有针对任何模组的专属补丁或硬编码兼容逻辑。

## 指令

所有 `/bingo*` 指令在 `PREGAME`（或对局中适用的状态）下要求配置权限。

| 指令 | 说明 |
|---|---|
| `/bingo` | 显示当前对局选项摘要 |
| `/bingo start`、`/bingo start ignore_warnings` | 开始对局（后者跳过数据包推荐人数等警告） |
| `/bingo end` | 结束当前对局（仅对局进行中可用） |
| `/bingo reset` | 重置对局与大厅状态（**仅大厅模式**，对局中或结算后可用） |
| `/bingo resume` | 恢复可继续的对局（**仅大厅模式**） |
| `/bingo nextround` | 结算后重置并直接开始下一轮（**仅大厅模式**，需有玩家在线） |
| `/bingo world seed`、`/bingo world seed <种子>`、`/bingo world seed random` | 查看 / 设置下一轮重置使用的世界种子（跨重启保留；`random` 恢复每轮随机） |
| `/bingo book` | 获取教程书 |
| `/bingo lobby`、`/bingo lobby confirm` | 切换到大厅模式（需确认） |
| `/bingo shuffleteams <队伍数>` | 赛前随机分队 |
| `/bingo goal <数量> <items\|lines>`、`/bingo goal full_card <items\|lines>` | 胜利目标 |
| `/bingo difficulty <S> <A> <B> <C> <D>`、`/bingo difficulty <预设名>` | 五个难度档位的格子数，或套用难度预设 |
| `/bingo timelimit <分钟>`、`/bingo timelimit off` | 对局时限 |
| `/bingo mode <lockout\|inventory\|hidden_items\|consume_items>` | 对局模式 |
| `/bingo options stalemate <end_game\|reroll_card\|do_nothing>` | 僵局处理 |
| `/bingo options end_when <never\|first_win\|teams_win\|all_win>` | 结束条件 |
| `/bingo options pvp [enabled]`、`elytra [enabled]`、`night_vision [enabled]` | 开关类规则 |
| `/bingo options spawn_distance <距离>`、`spawn_dimension <维度>` | 出生点设置 |
| `/bingo options play_to cards <数量>`、`play_to infinite_cards`、`play_to replace_goals` | 卡牌数量与进度规则 |
| `/bingo options preview_card` | 预览棋盘 |
| `/bingo card` | 查看棋盘 |
| `/bingo card set <格子> <目标>`、`... random`、`... free_space` | 逐格改目标（`b1`/`i3`/`o5` 坐标） |
| `/bingo card seed [种子]`、`/bingo card stack [push\|pop]`、`/bingo card assign <队伍> <棋盘>` | 种子、多棋盘队列与分配 |
| `/bingo reroll` | 重随当前棋盘 |
| `/bingo carddisable` / `/bingo cardenable` | 关闭 / 启用棋盘 |
| `/bingo filter [表达式]` | 按标签/条件过滤目标 |
| `/bingo autotier generate` / `clear` | 为未分类内容自动分级 |
| `/bingo debug` | 输出调试信息到服务端日志 |
| `/bingodata tag add\|remove`、`/bingodata tierlist add\|remove` | 就地编辑标签与难度表（支持通配符批量操作） |
| `/bingostats`、`/bingostats <玩家>`、`/bingostats reset player\|game\|all` | 统计数据查看与重置 |
| `/bingoprefs <偏好> [true\|false]` | 玩家个人偏好（HUD、计分板、提示消息等） |
| `/join <队伍>`、`/join spectators`、`/join <队伍> <玩家>` | 加入队伍 / 旁观 |
| `/ready`、`/ready cancel` | 准备 / 取消准备；结算后 `/ready` 作为「开始下一轮」投票 |
| `/spectator` | 切换旁观者模式（仅结算后） |
| `/teamchest`、`/tc` | 打开队伍共享箱 |
| `/teamtp <玩家>`、`/ttp <玩家>` | 传送到同队玩家 |
| `/tptoggle`、`/teamchest toggle`、`/teamchest count` | OP 在对局开始前切换相关功能 |
| `/teamchest teamrequest accept <申请人> <队伍>` | 同意旁观者的入队申请（申请人 UUID 由申请界面给出） |
| `/coords`、`/coords <说明>`、`/coords list` | 与队友共享坐标 |

## 按键

| 功能 | 默认键 |
|---|---|
| 查看 Bingo 卡片 | `Y` |
| 打开队伍共享箱 | `B` |
| 显示 / 隐藏 HUD | 未绑定（可在「控制」中设置） |

## 配置文件

服务端配置位于 `config/exbingo/`，可直接编辑，改完 `/reload` 或重启生效：

- `config.json` — 主配置（棋盘权重、队伍功能、统计、自动分级、已关闭棋盘等）
- `game-options.json` — 对局与棋盘选项（目标、分布、模式、出生点、结束条件）
- `tierlists/*.tierlist.json` — 难度表（S/A/B/C/D），自建即可定义自定义棋盘
- `tierlists/autotier.tierlist.json` — 由 `/bingo autotier generate` 生成
- `voicechat.json` — 语音分组设置（`enabled` 可整体关闭）
- `files.json` — 本模组对上述文件的追踪记录，用于判断文件是否被用户改过
- `commands/*.mcfunction` — 状态切换时执行的指令脚本（如 `on_countdown`）

NeoForge 侧另有两份 TOML（`config/exbingo-common.toml`、`config/exbingo-client.toml`）与 `config.json` 双向同步：**JSON 是运行时权威来源**，TOML 仅镜像可映射字段，且只在数值真正变化时回写。

## 构建与开发

```bash
./gradlew build          # 产物：build/libs/exbingo-<version>.jar
./gradlew test           # 单元测试
./gradlew runClient      # 开发客户端
./gradlew runServer      # 开发服务端（首次需 run/eula.txt）
./gradlew downloadAssets # 预下载客户端资源
```

需要 **JDK 21**。首次构建会执行 ModDevGradle/NeoForm 反编译管线，耗时较长。

CI：`.github/workflows/build-and-release.yml`。推送 `main` 只构建并跑测试；推送形如 `v1.1.1` 的 tag 会校验与 `gradle.properties` 的 `mod_version` 一致、构建并发布 GitHub Release（自动附带 jar 与 CHANGELOG 对应版本段落）；也可手动触发只构建。

## 文档

- [模组内容教程](模组内容教程/README.md) —— 如何把其他模组的内容引入棋盘
- [自定义棋盘教程](模组内容教程/自定义棋盘教程.md) —— 用 tierlist 精确指定棋盘内容
- [Bingo 指令教程](模组内容教程/Bingo指令教程.md) —— 指令速查

## 许可证与来源

ExBingo 是 Yet Another Bingo 的派生版本，以 `LGPL-3.0-only` 分发。队伍共享箱与队伍传送功能参考了 MIT 许可的 [YetAnotherBingo-TeamChest](https://github.com/ImCZFy/YetAnotherBingo-TeamChest)（Copyright (c) 2025 ChengZhiFy）。许可证文本见 [LICENSE](LICENSE) 与 [COPYING](COPYING)，来源与鸣谢见 [NOTICE](NOTICE)，版本历史见 [CHANGELOG.md](CHANGELOG.md)。

</details>

<details>
<summary>English</summary>

## Introduction

ExBingo is a **NeoForge 1.21.1** port of [Yet Another Bingo](https://modrinth.com/mod/yet-another-minecraft-bingo), providing a multiplayer Bingo item-hunt game mode. The port rewrote and adjusted much of the upstream mod, so some behavior differs.

**This mod ships Minecraft vanilla content only**: no per-mod boards, tier lists, or objective definitions, and no mod-specific code patches.

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.234 |
| License | LGPL-3.0-only |
| Runtime | Java 21 |

## Features

- **Multiplayer rounds**: teams, boards, lobby, countdown, and results flow.
- **Team chest**: a persistent shared chest per team, configurable for Bingo item scoring and consume-items mode. Cleared when the game resets.
- **Team teleport**: teammates can teleport to each other.
- **Spectator team join**: during a round a spectator can request to join a team; any player on that team can approve it.
- **Client HUD**: optional client-side Bingo card, messages, and team progress display.
- **Auto tiering**: generates a tier list for uncategorized content by obtain difficulty; vanilla content and already-assigned content are never touched.
- **Statistics**: game history, best times, win streaks. Stored in `config/exbingo/bingo.db` (SQLite) by default; external databases are supported.
- **Post-game ready & voting**: in lobby mode the results screen can ready up for the next round or start it immediately; while the ready countdown runs, players vote for **next round** or **return to lobby**, and the majority wins.
- **Long-game performance maintenance**: once the entity count passes a threshold, distant drops, projectiles, and ordinary mobs are cleaned up periodically; players, mounts, named/persistent/invulnerable entities, and protected namespaces are kept.

### Built-in boards

| Board ID | Name | Notes |
|---|---|---|
| `items` | Items | General item collection |
| `advancements` | Advancements | Advancement objectives |
| `simplified` | Simplified | A reduced objective set |
| `challenge` | Challenge | Challenge objectives (grouped collections, etc.) |

There are also **filters** (not boards): `everything`, `uncategorized`, `unobtainable`, plus tag sets such as `in_the_nether` / `in_the_end` for `/bingo filter`.

Operators can disable boards:

- `/bingo carddisable <board>` — disable it (hidden from the selection screen and "select all")
- `/bingo carddisable list` — list disabled boards
- `/bingo cardenable <board>` — re-enable it

The disabled set lives in `disabledFilterPresets` in `config/exbingo/config.json`; it applies immediately and survives restarts.

## Other mods' content

Items and advancements from other mods are **kept off cards by default**; two settings control this (`config/exbingo/config.json`):

| Setting | Default | Effect |
|---|---|---|
| `excludeModUncategorizedFromCards` | `true` | Uncategorized modded content is excluded; vanilla content and mod-provided categories are unaffected |
| `excludeUnbreakableBlocksFromCards` | `true` | Unbreakable blocks (bedrock, barriers, command blocks, ...) are excluded |

To opt content in:

- Namespace-wide: `/bingo filter +from=<modid> -uncategorized -unobtainable -unbreakable`
- Precise: write `tierlists/<name>.tierlist.json` and add a row to `itemFilterPresets` — see the [mod content guide](模组内容教程/README.md)
- Automatic: `/bingo autotier generate`, then enable it

## Mod integrations

Only the following 4 mods are integrated with. All are **optional and degrade gracefully** — nothing breaks when they are missing:

| Mod | Purpose | Declared as | When missing |
|---|---|---|---|
| **JEI** | Open item recipes from a card target | optional dependency in `neoforge.mods.toml` (client-only) | no recipe entry point |
| **Simple Voice Chat** | Team-based voice groups; winners move to spectators; cleanup on reset | optional dependency in `neoforge.mods.toml` | no grouping |
| **Xaero's Minimap** | Sync distant teammates during a round; clear the round's map cache and waypoints back in the lobby | runtime soft detection (`isModLoaded("xaerominimap")`) | no syncing |
| **Curios** | Clear accessory slots on game reset | NeoForge capability lookup `curios:item_handler` (no Curios classes referenced) | capability resolves to null, every call is a no-op |

Also note:

- **Permissions** do not require a permissions mod: built-in permission nodes (e.g. `exbingo.command.reset`) with defaults `ALLOW` / `OPERATORS` / `DENY`, where `OPERATORS` means op level 2 (singleplayer counts as op).
- Other than the table above there are no mod-specific patches or hardcoded compatibility paths.

## Commands

All `/bingo*` commands require configuration permission in `PREGAME` (or the states where they apply).

| Command | Description |
|---|---|
| `/bingo` | Print the current round options |
| `/bingo start`, `/bingo start ignore_warnings` | Start a round (the latter skips datapack player-count warnings) |
| `/bingo end` | End the current round (only while running) |
| `/bingo reset` | Reset the round and lobby state (**lobby mode only**, while playing or after results) |
| `/bingo resume` | Resume a resumable round (**lobby mode only**) |
| `/bingo nextround` | Reset and immediately start the next round (**lobby mode only**, requires players online) |
| `/bingo world seed`, `/bingo world seed <seed>`, `/bingo world seed random` | Show/set the seed used by the next world reset (kept across restarts; `random` restores per-round randomization) |
| `/bingo book` | Get the tutorial book |
| `/bingo lobby`, `/bingo lobby confirm` | Switch to lobby mode (with confirmation) |
| `/bingo shuffleteams <count>` | Randomly split teams before the round |
| `/bingo goal <count> <items\|lines>`, `/bingo goal full_card <items\|lines>` | Win goal |
| `/bingo difficulty <S> <A> <B> <C> <D>`, `/bingo difficulty <preset>` | Tier counts per difficulty, or apply a preset |
| `/bingo timelimit <minutes>`, `/bingo timelimit off` | Round time limit |
| `/bingo mode <lockout\|inventory\|hidden_items\|consume_items>` | Game mode |
| `/bingo options stalemate <end_game\|reroll_card\|do_nothing>` | Stalemate behavior |
| `/bingo options end_when <never\|first_win\|teams_win\|all_win>` | End conditions |
| `/bingo options pvp [enabled]`, `elytra [enabled]`, `night_vision [enabled]` | Toggle rules |
| `/bingo options spawn_distance <distance>`, `spawn_dimension <dimension>` | Spawn settings |
| `/bingo options play_to cards <count>`, `play_to infinite_cards`, `play_to replace_goals` | Card count and progress rules |
| `/bingo options preview_card` | Preview the card |
| `/bingo card` | Inspect the card |
| `/bingo card set <tile> <objective>`, `... random`, `... free_space` | Set a single tile (`b1`/`i3`/`o5`) |
| `/bingo card seed [seed]`, `/bingo card stack [push\|pop]`, `/bingo card assign <team> <card>` | Seed, card queue, and assignment |
| `/bingo reroll` | Reroll the current card |
| `/bingo carddisable` / `/bingo cardenable` | Disable / enable a board |
| `/bingo filter [expression]` | Filter objectives by tags/conditions |
| `/bingo autotier generate` / `clear` | Auto-tier uncategorized content |
| `/bingo debug` | Write debug info to the server log |
| `/bingodata tag add\|remove`, `/bingodata tierlist add\|remove` | Edit tags and tier lists in place (wildcards supported) |
| `/bingostats`, `/bingostats <player>`, `/bingostats reset player\|game\|all` | View and reset statistics |
| `/bingoprefs <pref> [true\|false]` | Per-player preferences (HUD, scoreboard, notifications, ...) |
| `/join <team>`, `/join spectators`, `/join <team> <player>` | Join a team / spectate |
| `/ready`, `/ready cancel` | Ready up / cancel; after results, `/ready` votes to start the next round |
| `/spectator` | Toggle spectator mode (postgame only) |
| `/teamchest`, `/tc` | Open the team chest |
| `/teamtp <player>`, `/ttp <player>` | Teleport to a teammate |
| `/tptoggle`, `/teamchest toggle`, `/teamchest count` | Operators toggle these features before a round starts |
| `/teamchest teamrequest accept <requester> <team>` | Approve a spectator's join request (the requester UUID is provided by the request screen) |
| `/coords`, `/coords <note>`, `/coords list` | Share coordinates with your team |

## Keybinds

| Action | Default key |
|---|---|
| View Bingo Card | `Y` |
| Open Team Chest | `B` |
| Toggle Bingo HUD | unbound (set it in Controls) |

## Configuration

Server configuration lives in `config/exbingo/` and can be edited directly; run `/reload` or restart to apply:

- `config.json` — main config (board weights, team features, stats, auto-tiering, disabled boards, ...)
- `game-options.json` — round and card options (goal, distribution, modes, spawn, end conditions)
- `tierlists/*.tierlist.json` — tier lists (S/A/B/C/D); add your own to define custom boards
- `tierlists/autotier.tierlist.json` — generated by `/bingo autotier generate`
- `voicechat.json` — voice group settings (`enabled` turns the integration off)
- `files.json` — tracking records used to tell whether a file was edited by the user
- `commands/*.mcfunction` — command scripts run on state changes (e.g. `on_countdown`)

Two NeoForge TOMLs (`config/exbingo-common.toml`, `config/exbingo-client.toml`) mirror the mappable fields and sync both ways: **the JSON file is the runtime source of truth**, and the TOML is rewritten only when a mapped value actually changes.

## Building and development

```bash
./gradlew build          # output: build/libs/exbingo-<version>.jar
./gradlew test           # unit tests
./gradlew runClient      # development client
./gradlew runServer      # development server (run/eula.txt on first launch)
./gradlew downloadAssets # pre-download client assets
```

Requires **JDK 21**. The first build runs the ModDevGradle/NeoForm decompile pipeline and takes a while.

CI: `.github/workflows/build-and-release.yml`. Pushes to `main` build and test only; pushing a `v1.1.1`-style tag verifies it against `mod_version`, builds, and publishes a GitHub Release with the jar and the matching CHANGELOG section. Manual dispatch builds without releasing.

## Documentation

- [Mod content guide](模组内容教程/README.md) — bringing other mods' content into boards
- [Custom board tutorial](模组内容教程/自定义棋盘教程.md) — precise boards via tier lists
- [Commands reference](模组内容教程/Bingo指令教程.md) — command cheat sheet

## License and attribution

ExBingo is a derivative of Yet Another Bingo and is distributed under `LGPL-3.0-only`. The team chest and team teleport features reference the MIT-licensed [YetAnotherBingo-TeamChest](https://github.com/ImCZFy/YetAnotherBingo-TeamChest) project (Copyright (c) 2025 ChengZhiFy). See [LICENSE](LICENSE) and [COPYING](COPYING) for license texts, [NOTICE](NOTICE) for sources and credits, and [CHANGELOG.md](CHANGELOG.md) for release history.

</details>
