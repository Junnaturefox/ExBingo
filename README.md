# ExBingo

<details open>
<summary>中文说明</summary>

## 这是什么

ExBingo 是一个多人 **Bingo（宾果）** 小游戏模组：给每支队伍发一张 5×5 的卡片，格子写着要收集的物品或要完成的进度，谁先完成目标（连成指定的线数，或收齐指定数量的物品）谁就赢。

它是 [Yet Another Bingo](https://modrinth.com/mod/yet-another-minecraft-bingo) 的 NeoForge 1.21.1 移植版。移植时做了较多重写和调整，所以部分细节与原版不同。

模组**只使用 Minecraft 原版内容**：不自带任何其他模组的专属棋盘、难度表或目标，也不针对某个模组写死补丁。想用其他模组的东西，可以按[教程](模组内容教程/README.md)自己加进来。

## 安装要求

| 项目 | 要求 |
|---|---|
| Minecraft | 1.21.1 |
| 模组加载器 | NeoForge 21.1.234 |
| Java | 21 |
| 许可证 | LGPL-3.0-only |

客户端和服务端都需要装本模组（单人游戏也要装）。

## 一局是怎么玩的

1. 进服后先用 `/join <队伍名>` 选队伍，或者由管理员执行 `/bingo shuffleteams <队伍数>` 随机分队。
2. 大家准备好后，管理员执行 `/bingo start`，或者在大厅菜单里点「开始」；也可以让准备倒计时到点自动开始。
3. 每队查看自己的卡片（按 `Y` 或输入 `/bingo card`），分头去完成格子上的目标。
4. 先达到胜利目标的队伍获胜；对局结束会显示结算界面。
5. 之后可以「准备下一轮」等倒计时，也可以在结算界面直接点「下一轮」立刻开下一局。

## 玩的时候能用到的功能

- **队伍与大厅**：分队、大厅集合、开局倒计时、结算界面一条龙。
- **队伍共享箱**：每队一个共享箱子，队员都能存取；可以设置箱子里的物品算不算完成 Bingo 物品格。每局重置时清空。
- **同队传送**：`/teamtp <玩家>`（也可以 `/ttp`）直接传到队友身边。
- **旁观者申请入队**：对局中没加入队伍的旁观者可以申请加入某队，队内玩家点同意即可。
- **界面显示（HUD）**：装了模组后屏幕上有简易卡片、得分提示和队伍进度；HUD 可以按键显示/隐藏。
- **战绩统计**：记录历史对局、最佳用时和连胜，默认存在 `config/exbingo/bingo.db`。
- **赛后备战与投票**：结算后可以准备下一轮；准备倒计时期间还能投票选「下一轮」或「返回大厅」，按多数票执行。
- **长时间对局自动减压**：一局打得很久、实体太多时，服务器会自动清理离玩家很远的掉落物、飞箭和普通生物，玩家、坐骑以及命名/持久/无敌的实体不受影响。

## 内置棋盘

游戏里能选的棋盘有这 4 个：

| 棋盘 | 里面是什么 |
|---|---|
| 物品（`items`） | 通用物品收集 |
| 进度（`advancements`） | 原版成就类目标 |
| 精简（`simplified`） | 数量更少的精简目标 |
| 挑战（`challenge`） | 成组收集之类的挑战目标 |

另外还有一些**筛选条件**（不是棋盘本身），例如「全部」「未分类」「无法获得」，以及「下界」「末地」等标签集合，配合 `/bingo filter` 使用。

管理员可以关掉不想用的棋盘：

- `/bingo carddisable <棋盘>` —— 关掉它，玩家在选择界面和「全选」里都看不到
- `/bingo carddisable list` —— 查看已经关掉了哪些
- `/bingo cardenable <棋盘>` —— 重新启用

关掉的状态会保存在配置里，改完立即生效、重启也在。

## 关于其他模组的内容

装了其他模组后，它们的物品和进度**默认不会出现在卡片上**，这是为了避免棋盘被大量模组内容淹没。想用的话有两个办法：

- **简单办法**：用筛选条件把某个模组的内容选进来，例如
  `/bingo filter +from=<模组id> -uncategorized -unobtainable -unbreakable`
- **精确办法**：自己写一份棋盘文件，逐个指定目标与难度 + 在配置里登记，详见[自定义棋盘教程](模组内容教程/自定义棋盘教程.md)

如果内容太多懒得逐个分类，可以先跑 `/bingo autotier generate` 让模组自动按获取难度分档，再按需调整。

配置里有两个默认开启的开关会影响结果：未分类的模组内容不进卡片、不可破坏的方块（基岩、屏障、命令方块等）不进卡片。不需要时可以在配置里关掉。

## 可以一起用的模组

下面 4 个模组都是**可选**的，装了就多出对应功能，不装也完全能正常玩：

| 模组 | 装了会怎样 |
|---|---|
| **JEI** | 在卡片目标上直接查看该物品的合成配方 |
| **Simple Voice Chat** | 按队伍自动分语音频道，获胜方转入旁观；对局结束会清理 |
| **Xaero 的小地图** | 对局中能看到远处队友的位置；回到大厅会清理本局的地图缓存和路径点 |
| **Curios** | 每局重置时清空饰品栏 |

除了这 4 个，本模组没有针对其他模组的专属兼容或补丁。

## 权限

不需要额外安装权限模组。模组自带权限设定，默认是「管理员（OP）可以改游戏设置、普通玩家只能正常参与」；单人游戏视作有权限。如果想用权限模组细调，节点名形如 `exbingo.command.reset`。

## 指令

大部分 `/bingo` 开头的指令需要管理员权限，且只在准备阶段（或该指令适用的阶段）有效。

| 指令 | 作用 |
|---|---|
| `/bingo` | 查看当前对局设置 |
| `/bingo start`、`/bingo start ignore_warnings` | 开始对局；后者会忽略人数之类的警告强行开始 |
| `/bingo end` | 结束当前对局（仅进行中可用） |
| `/bingo reset` | 重置对局并回到大厅（仅大厅模式，进行中或结算后可用） |
| `/bingo resume` | 继续上次没打完的对局（仅大厅模式） |
| `/bingo nextround` | 结算后重置并直接开始下一轮（仅大厅模式，且有玩家在线） |
| `/bingo world seed`、`/bingo world seed <种子>`、`/bingo world seed random` | 查看 / 指定下一局生成地图用的种子；`random` 恢复每局随机。设置重启后仍保留 |
| `/bingo book` | 拿一本教程书 |
| `/bingo lobby`、`/bingo lobby confirm` | 把服务器切换到大厅模式（需要确认） |
| `/bingo shuffleteams <队伍数>` | 赛前随机分队，玩家会尽量平均分配 |
| `/bingo goal <数量> <items\|lines>`、`/bingo goal full_card <items\|lines>` | 设置胜利目标（收集多少物品 / 连成多少线） |
| `/bingo difficulty <S> <A> <B> <C> <D>`、`/bingo difficulty <预设名>` | 调整各难度档位放多少格，或直接用预设 |
| `/bingo timelimit <分钟>`、`/bingo timelimit off` | 设置对局时间上限 / 取消上限 |
| `/bingo mode <lockout\|inventory\|hidden_items\|consume_items>` | 选择对局模式 |
| `/bingo options stalemate <end_game\|reroll_card\|do_nothing>` | 僵局时怎么处理 |
| `/bingo options end_when <never\|first_win\|teams_win\|all_win>` | 什么时候结束对局 |
| `/bingo options pvp [enabled]`、`elytra [enabled]`、`night_vision [enabled]` | 开关 PvP、鞘翅、夜视 |
| `/bingo options spawn_distance <距离>`、`spawn_dimension <维度>` | 各队出生点的分布距离与所在维度 |
| `/bingo options play_to cards <数量>`、`play_to infinite_cards`、`play_to replace_goals` | 打几张卡、无限换卡、完成后再换目标 |
| `/bingo options preview_card` | 预览当前卡片 |
| `/bingo card` | 查看自己的卡片 |
| `/bingo card set <格子> <目标>`、`... random`、`... free_space` | 手动改某一格（格子坐标如 `b1`、`i3`、`o5`） |
| `/bingo card seed [种子]`、`/bingo card stack [push\|pop]`、`/bingo card assign <队伍> <棋盘>` | 卡片种子、多卡队列与分配 |
| `/bingo reroll` | 重掷当前卡片 |
| `/bingo carddisable` / `/bingo cardenable` | 关闭 / 启用某个棋盘 |
| `/bingo filter [表达式]` | 按标签或条件筛选卡片目标 |
| `/bingo autotier generate` / `clear` | 自动为未分类内容分难度 / 清除生成结果 |
| `/bingo debug` | 把调试信息输出到服务端日志（排查问题用） |
| `/bingodata tag add\|remove`、`/bingodata tierlist add\|remove` | 直接增删标签和难度表条目（支持通配符） |
| `/bingostats`、`/bingostats <玩家>`、`/bingostats reset player\|game\|all` | 查看与重置战绩 |
| `/bingoprefs <偏好> [true\|false]` | 个人偏好（HUD、计分板、提示消息等） |
| `/join <队伍>`、`/join spectators`、`/join <队伍> <玩家>` | 加入队伍 / 当旁观者 / 帮别人换队 |
| `/ready`、`/ready cancel` | 准备 / 取消准备；结算后的倒计时里，`/ready` 等于投「开始下一轮」一票 |
| `/spectator` | 切换旁观模式（仅结算后） |
| `/teamchest`、`/tc` | 打开队伍共享箱 |
| `/teamtp <玩家>`、`/ttp <玩家>` | 传送到队友身边 |
| `/tptoggle`、`/teamchest toggle`、`/teamchest count` | 开局前开关队伍传送、队伍箱、箱子物品是否计入目标 |
| `/teamchest teamrequest accept <申请人> <队伍>` | 同意某位旁观者的入队申请（申请人信息由申请界面给出） |
| `/coords`、`/coords <说明>`、`/coords list` | 把自己坐标（可带说明）发给队友，或查看已分享的坐标 |

## 按键

| 功能 | 默认按键 |
|---|---|
| 查看 Bingo 卡片 | `Y` |
| 打开队伍共享箱 | `B` |
| 显示 / 隐藏 HUD | 默认没绑，可在「选项 → 控制」里设置 |

## 配置文件

一般不用改。想调整默认设置的话，服务端文件都在 `config/exbingo/` 下，改完执行 `/reload` 或重启生效：

- `config.json` —— 主要设置（棋盘权重、队伍功能、统计、自动分级、被关闭的棋盘等）
- `game-options.json` —— 每一局的默认规则（胜利目标、格子分布、模式、出生点、结束条件）
- `tierlists/*.tierlist.json` —— 难度表，自己写一份就能做出自定义棋盘
- `tierlists/autotier.tierlist.json` —— 由 `/bingo autotier generate` 自动生成
- `voicechat.json` —— 语音分组设置（关掉就不会分频道）
- `files.json` —— 模组自己用来判断文件是否被玩家改过，别手动编辑
- `commands/*.mcfunction` —— 在特定阶段自动执行的指令脚本（例如 `on_countdown`）

游戏内的模组设置界面和 `config.json` 会互相同步，以 `config.json` 为准；只有值真的变化时才会写回文件。

## 自己编译（可选）

```bash
./gradlew build          # 生成 build/libs/exbingo-<版本>.jar
./gradlew test           # 跑单元测试
./gradlew runClient      # 启动开发用客户端
./gradlew runServer      # 启动开发用服务端（首次需创建 run/eula.txt）
./gradlew downloadAssets # 预先下载客户端资源
```

需要 **JDK 21**。第一次编译会下载并反编译游戏本体，耗时较久。

## 更多文档

- [模组内容教程](模组内容教程/README.md) —— 怎么把其他模组的内容加进棋盘
- [自定义棋盘教程](模组内容教程/自定义棋盘教程.md) —— 精确指定卡片上的每一个目标
- [Bingo 指令教程](模组内容教程/Bingo指令教程.md) —— 指令速查

## 来源与许可

ExBingo 基于 Yet Another Bingo，以 `LGPL-3.0-only` 许可分发。队伍共享箱与队伍传送功能参考了 MIT 许可的 [YetAnotherBingo-TeamChest](https://github.com/ImCZFy/YetAnotherBingo-TeamChest)（Copyright (c) 2025 ChengZhiFy）。许可全文见 [LICENSE](LICENSE) 与 [COPYING](COPYING)，来源鸣谢见 [NOTICE](NOTICE)，版本更新见 [CHANGELOG.md](CHANGELOG.md)。

</details>

<details>
<summary>English</summary>

## What this is

ExBingo is a multiplayer **Bingo** game mode for Minecraft: every team gets a 5×5 card filled with items to collect or advancements to earn, and the first team to finish the goal (complete a number of lines, or collect a number of items) wins.

It is a NeoForge 1.21.1 port of [Yet Another Bingo](https://modrinth.com/mod/yet-another-minecraft-bingo). The port rewrote and adjusted a lot, so some details differ from the original.

The mod uses **vanilla Minecraft content only**: no per-mod boards, tier lists or objectives, and no hardcoded patches for specific mods. To use another mod's content, add it yourself with the [guides](模组内容教程/README.md).

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Mod loader | NeoForge 21.1.234 |
| Java | 21 |
| License | LGPL-3.0-only |

The mod is needed on both the client and the server (even in singleplayer).

## How a round works

1. Join a team with `/join <team>`, or let an operator split everyone with `/bingo shuffleteams <count>`.
2. When everyone is ready an operator runs `/bingo start` (or clicks "Start" in the lobby menu); the ready countdown can also start it automatically.
3. Each team looks at its card (`Y`, or `/bingo card`) and goes after the goals.
4. The first team to reach the win goal wins, and the results screen appears.
5. From there you can ready up for the next round, or click "Next Round" to start it immediately.

## What you get in game

- **Teams and lobby**: team splitting, a lobby to gather in, a start countdown and a results screen.
- **Team chest**: one shared chest per team; you can choose whether its items count towards Bingo item tiles. It is emptied when the round resets.
- **Team teleport**: `/teamtp <player>` (or `/ttp`) takes you to a teammate.
- **Spectator join requests**: a spectator without a team can ask to join one, and any member can approve.
- **On-screen HUD**: a compact card, score messages and team progress, with a keybind to show/hide it.
- **Statistics**: past games, best times and win streaks, stored in `config/exbingo/bingo.db` by default.
- **Post-game ready and voting**: after the results you can ready up for the next round, and during the ready countdown you can vote for **next round** or **return to lobby**; the majority decides.
- **Long-round cleanup**: in very long rounds the server periodically removes far-away drops, arrows and ordinary mobs once there are too many entities. Players, mounts and named/persistent/invulnerable entities are kept.

## Built-in boards

There are four boards to choose from:

| Board | What is on it |
|---|---|
| Items (`items`) | General item collection |
| Advancements (`advancements`) | Vanilla advancement goals |
| Simplified (`simplified`) | A smaller, easier set |
| Challenge (`challenge`) | Challenge goals such as grouped collections |

There are also **filters** (not boards), such as "everything", "uncategorized" and "unobtainable", plus tag sets like "nether" and "the end", used with `/bingo filter`.

Operators can disable boards they do not want:

- `/bingo carddisable <board>` — hide it from the selection screen and from "select all"
- `/bingo carddisable list` — list the disabled boards
- `/bingo cardenable <board>` — turn it back on

The disabled list is saved in the config, applies immediately and survives restarts.

## Other mods' content

Items and advancements from other mods are **kept off cards by default**, so cards do not get flooded with mod content. To include them:

- **Quick way**: filter a mod's content in, e.g.
  `/bingo filter +from=<modid> -uncategorized -unobtainable -unbreakable`
- **Precise way**: write your own board file with exactly the goals and tiers you want and register it; see the [custom board guide](模组内容教程/自定义棋盘教程.md)

If there is too much content to sort by hand, run `/bingo autotier generate` to let the mod estimate difficulty, then adjust.

Two settings (both on by default) affect the result: uncategorized mod content stays off cards, and unbreakable blocks (bedrock, barriers, command blocks, ...) stay off cards. You can turn either off in the config.

## Optional mod integrations

All four are **optional**: install them for the extra features, or play without them.

| Mod | What changes |
|---|---|
| **JEI** | Open an item's recipe directly from a card target |
| **Simple Voice Chat** | Teams get their own voice channel; the winning team moves to spectators; cleaned up when the round ends |
| **Xaero's Minimap** | Distant teammates show up during a round; the round's map cache and waypoints are cleared back in the lobby |
| **Curios** | Accessory slots are cleared when the round resets |

Apart from these four, there are no mod-specific patches or compatibility layers.

## Permissions

You do not need a permissions mod. Permissions are built in: operators can change game settings, everyone else can play as usual, and singleplayer counts as operator. If you want finer control with a permissions mod, the node names look like `exbingo.command.reset`.

## Commands

Most `/bingo` commands need operator permission and only work before a round starts (or in the phase they apply to).

| Command | What it does |
|---|---|
| `/bingo` | Show the current round settings |
| `/bingo start`, `/bingo start ignore_warnings` | Start the round; the latter ignores warnings such as the recommended player count |
| `/bingo end` | End the current round (only while it is running) |
| `/bingo reset` | Reset the round and return to the lobby (lobby mode only, while playing or after results) |
| `/bingo resume` | Continue a round that can be resumed (lobby mode only) |
| `/bingo nextround` | After results, reset and start the next round immediately (lobby mode only, at least one player online) |
| `/bingo world seed`, `/bingo world seed <seed>`, `/bingo world seed random` | Show/set the seed used to generate the next round's world; `random` restores per-round randomization. Kept across restarts |
| `/bingo book` | Get the tutorial book |
| `/bingo lobby`, `/bingo lobby confirm` | Switch the server to lobby mode (with confirmation) |
| `/bingo shuffleteams <count>` | Split players into teams before the round, as evenly as possible |
| `/bingo goal <count> <items\|lines>`, `/bingo goal full_card <items\|lines>` | Set the win goal (items collected or lines completed) |
| `/bingo difficulty <S> <A> <B> <C> <D>`, `/bingo difficulty <preset>` | Set how many tiles each difficulty uses, or apply a preset |
| `/bingo timelimit <minutes>`, `/bingo timelimit off` | Set or remove the round time limit |
| `/bingo mode <lockout\|inventory\|hidden_items\|consume_items>` | Choose the game mode |
| `/bingo options stalemate <end_game\|reroll_card\|do_nothing>` | What happens on a stalemate |
| `/bingo options end_when <never\|first_win\|teams_win\|all_win>` | When the round ends |
| `/bingo options pvp [enabled]`, `elytra [enabled]`, `night_vision [enabled]` | Toggle PvP, elytra and night vision |
| `/bingo options spawn_distance <distance>`, `spawn_dimension <dimension>` | How far apart teams spawn, and in which dimension |
| `/bingo options play_to cards <count>`, `play_to infinite_cards`, `play_to replace_goals` | How many cards a round lasts, endless cards, or replace goals after completion |
| `/bingo options preview_card` | Preview the current card |
| `/bingo card` | Open your card |
| `/bingo card set <tile> <objective>`, `... random`, `... free_space` | Edit a single tile (coordinates like `b1`, `i3`, `o5`) |
| `/bingo card seed [seed]`, `/bingo card stack [push\|pop]`, `/bingo card assign <team> <card>` | Card seed, card queue and assignment |
| `/bingo reroll` | Reroll the current card |
| `/bingo carddisable` / `/bingo cardenable` | Disable / enable a board |
| `/bingo filter [expression]` | Filter card objectives by tags or conditions |
| `/bingo autotier generate` / `clear` | Auto-tier uncategorized content / remove the result |
| `/bingo debug` | Write debug information to the server log (for troubleshooting) |
| `/bingodata tag add\|remove`, `/bingodata tierlist add\|remove` | Add or remove tags and tier list entries in place (wildcards supported) |
| `/bingostats`, `/bingostats <player>`, `/bingostats reset player\|game\|all` | View and reset statistics |
| `/bingoprefs <pref> [true\|false]` | Per-player preferences (HUD, scoreboard, notifications, ...) |
| `/join <team>`, `/join spectators`, `/join <team> <player>` | Join a team / spectate / move another player |
| `/ready`, `/ready cancel` | Ready up / cancel; during the post-game countdown, `/ready` is a vote for the next round |
| `/spectator` | Toggle spectator mode (post-game only) |
| `/teamchest`, `/tc` | Open the team chest |
| `/teamtp <player>`, `/ttp <player>` | Teleport to a teammate |
| `/tptoggle`, `/teamchest toggle`, `/teamchest count` | Before a round: toggle team teleport, the team chest, and whether chest items count |
| `/teamchest teamrequest accept <requester> <team>` | Approve a spectator's join request (the requester details come from the request screen) |
| `/coords`, `/coords <note>`, `/coords list` | Share your coordinates (optionally with a note) with your team, or list shared ones |

## Keybinds

| Action | Default key |
|---|---|
| View Bingo card | `Y` |
| Open team chest | `B` |
| Show / hide HUD | unbound by default; set it in Options → Controls |

## Configuration

You normally do not need to touch these. If you want to change defaults, the server files live in `config/exbingo/`; run `/reload` or restart after editing:

- `config.json` — main settings (board weights, team features, statistics, auto-tiering, disabled boards, ...)
- `game-options.json` — default round rules (win goal, tile distribution, modes, spawn, end conditions)
- `tierlists/*.tierlist.json` — difficulty tier lists; write your own to define a custom board
- `tierlists/autotier.tierlist.json` — generated by `/bingo autotier generate`
- `voicechat.json` — voice grouping settings (disable it to stop creating voice channels)
- `files.json` — used internally to tell whether a file was edited by a player; do not edit it
- `commands/*.mcfunction` — scripts run automatically at certain phases (e.g. `on_countdown`)

The in-game mod settings screen and `config.json` stay in sync, with `config.json` as the source of truth; the file is only rewritten when a value actually changes.

## Building from source (optional)

```bash
./gradlew build          # produces build/libs/exbingo-<version>.jar
./gradlew test           # run unit tests
./gradlew runClient      # launch a development client
./gradlew runServer      # launch a development server (create run/eula.txt first)
./gradlew downloadAssets # pre-download client assets
```

Requires **JDK 21**. The first build downloads and decompiles the game, so it takes a while.

## More documentation

- [Mod content guide](模组内容教程/README.md) — bringing other mods' content into cards
- [Custom board tutorial](模组内容教程/自定义棋盘教程.md) — specifying every card goal precisely
- [Commands reference](模组内容教程/Bingo指令教程.md) — command cheat sheet

## Sources and license

ExBingo is a derivative of Yet Another Bingo and is distributed under `LGPL-3.0-only`. The team chest and team teleport features reference the MIT-licensed [YetAnotherBingo-TeamChest](https://github.com/ImCZFy/YetAnotherBingo-TeamChest) (Copyright (c) 2025 ChengZhiFy). See [LICENSE](LICENSE) and [COPYING](COPYING) for the license texts, [NOTICE](NOTICE) for sources and credits, and [CHANGELOG.md](CHANGELOG.md) for release history.

</details>
