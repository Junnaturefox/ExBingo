# ExBingo Changelog / ExBingo 更新日志

本文件记录 ExBingo 每次正式发布的更新内容。新版本写在最上方，保持中英文双语。
This file records the release notes for ExBingo. New releases go on top, kept bilingual.

## 写法要求 / How to write these notes

- 用**普通玩家能看懂的话**写：先说玩家会感受到什么变化，再说怎么用；不要写代码类名、内部文件路径或实现细节。
- 涉及玩家需要照做的操作（例如某个配置文件、某条指令）时，把名字写清楚。
- 中文在前、英文在后，分类一一对应。
- 分类顺序：`新增内容` → `调整内容` → `修复内容` → `兼容性`（英文 `Added` / `Changed` / `Fixed` / `Compatibility`）；某类没有内容就省略。
- 每个版本之间用单独一行 `——————` 分隔。
- Write in plain player-facing language: what a player will notice, and how to use it. Avoid class names, internal paths and implementation details; keep names only when the player has to use them.
- Chinese first, English second, with matching sections. Use `——————` between releases.

——————

## 1.2.1 - 2026-11-28

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Bug fix

### 中文更新日志

#### 修复内容

- **对局进度现在真的会跟着世界存档一起保存了**。1.2.0 的更新说明里写了这一点，但那条保存钩子当时并没有接上，所以实际上只有切换阶段和关服时才会写盘；现在服务器自动存档、`/save-all` 和强制关服都会保存你的进度。
- 修复「用旧版本打开新版本的存档会把进度写没」的问题。现在遇到比当前版本更新的存档，游戏会把它原样保留成一个 `.newer` 文件（不会解析、也不会覆盖），并用一份全新配置继续运行；升级回新版本后把文件名改回去即可找回。
- 修复大厅菜单偶尔「左半边点不动」的问题。上一局残留的菜单实体（有时在世界加载完成后才出现）现在会被稳定清理掉，不会继续挡在真实按钮前面。
- 修复关闭或删除大厅世界后，队伍选择占用的区块记录会累积下来、可能作用到下一个大厅世界的问题。
- 修复部分情况下 Xaero 小地图的回大厅缓存清理没有触发的问题（当前版本客户端只接收新版数据包，而清理逻辑只监听了旧版）。
- 修复服务器重启或重新开图后，个别玩家可能卡在「加载中」的问题。地形加载的判定数据现在按服务器实例隔离，不会带上一次运行留下的状态。

#### 兼容性

- **只与当前版本和上一版（1.2.0）的客户端互通**。更早版本的客户端仍然能进服游玩，但收不到新版界面数据，只会退化为基础提示（例如结算提示改为聊天/标题显示）；反过来，旧服务器也读不到新客户端的新数据包。请让服务端与客户端使用同一版本。
- 仍为 Minecraft 1.21.1 / NeoForge 21.1.234，存档与配置文件可直接沿用（第一次启动会自动把存档升级为新格式）。
- 与 JEI、Simple Voice Chat、Xaero 小地图、Curios 的联动行为不变。

### English Changelog

#### Fixed

- **Round progress is now actually saved together with the world save.** The 1.2.0 notes claimed this, but the save hook was never wired up, so progress was only written on phase changes and shutdown; server autosaves, `/save-all` and a forced shutdown now all save your progress.
- Fixed older builds overwriting the progress of a save written by a newer build. Such a save is now left untouched and preserved as a `.newer` file (never parsed, never overwritten) while the game runs on fresh defaults; rename it back after upgrading to recover it.
- Fixed the lobby menu occasionally not responding on its left half. Menu entities left over from the previous round (sometimes appearing only after the world finished loading) are now cleaned up reliably instead of sitting in front of the real buttons.
- Fixed the chunk records held for the team picker piling up after the lobby world was closed or deleted and potentially affecting the next lobby world.
- Fixed Xaero's Minimap cache cleanup not triggering in some cases (current clients only receive the new packet, while the cleanup only listened for the old one).
- Fixed individual players occasionally getting stuck on "loading" after a server restart or world regeneration. The terrain-loading signals are now isolated per server instance, so a previous run's state is never reused.

#### Compatibility

- **Only the current and the previous (1.2.0) client versions interoperate.** Older clients can still join and play, but they do not receive the new UI data and fall back to basic messages (e.g. results shown as chat/title instead of the results screen); the same applies in reverse for old servers reading new clients. Run the same version on both sides.
- Still Minecraft 1.21.1 / NeoForge 21.1.234; saves and configs carry over (the save format is upgraded automatically on first launch).
- JEI, Simple Voice Chat, Xaero's Minimap and Curios integrations behave as before.

——————

## 1.2.0 - 2026-11-28

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Feature update

### 中文更新日志

#### 新增内容

- **结算后直接开下一轮**：结算界面上新增「下一轮」按钮，也可以输入 `/bingo nextround`。点一下就会重置世界并直接开始下一局，不用再回大厅重新准备（仅大厅模式，且需要至少一人在线）。
- **赛后投票**：结算后的准备倒计时期间，界面上会出现「下一轮」和「返回大厅」两个投票按钮，按钮上显示当前票数、你投过的那个会打勾。倒计时结束时按多数票决定去向；没人投票时回到大厅。
- **世界种子指令**：新增 `/bingo world seed`，可以查看下一局将使用的地形种子；`/bingo world seed <种子>` 指定种子，`/bingo world seed random` 恢复每局随机。设置会一直保留（重启也不丢），方便反复练习同一张地图。
- **更完整的记分板**：赛前显示每支队伍的人数，赛后显示各队得分，准备倒计时期间还会显示「已准备 X/Y」。
- **给其他模组的接口**：其他模组现在可以读取当前棋盘的信息（格子上的目标、所属难度档位、卡片规则），方便做联动或显示类模组。

#### 调整内容

- `/bingo shuffleteams <队伍数>` 现在严格按你写的队伍数分队，并把玩家尽量平均分配到各队（例如 7 人 3 队会分成 3/2/2，不会再出现一队只有 1 人被三队围攻）。
- 高分屏、Retina 屏以及把界面缩放调大时，HUD 上的点击位置更准确了（以前会整体偏移）。
- 服务器长期运行时，离线玩家的数据不再一直堆在内存里。
- 结算界面、旁观者入队界面的刷新频率降低，减少无谓的卡顿和网络流量。
- 对局进度现在会跟随世界存档一起保存，服务器崩溃或强退时损失的进度更少。

#### 修复内容

- **修复 `/bingo start` 在没有玩家在线时会卡死**：以前会停在「准备中」，之后没有任何指令能退出；现在会直接提示先让玩家加入。
- 修复 `/bingo start` 失败时仍然提示「正在开始游戏」的问题。
- 修复 `/ready cancel` 取消后准备倒计时立刻重新开始、实际取消不掉的问题。
- 修复准备倒计时到点自动开局时，一旦条件不满足就无限重试并刷屏的问题。
- 修复正式版里 `/bingo debug` 用不了（以前只在测试版存在）以及在正式版调用时会崩溃的问题。
- 修复 `/bingo shuffleteams` 在没有玩家在线时崩溃、并把原有队伍清空的问题。
- 修复 `/bingoprefs hide_lobby_prompt` 无法切换的问题。
- 修复 `/join` 在无权限或非准备阶段「凭空创建队伍」的问题；大厅里点击队伍牌子现在也需要加入队伍权限。
- 修复控制台或命令方块执行 `/ready`、`/coords`、`/bingo lobby` 等指令时报错并刷日志的问题。
- 修复卡片生成失败时导致服务器崩溃的问题：现在会保留原来的卡片或退回一张默认卡片，并给出提示。
- 修复窄窗口/竖屏下打开「卡片位置」设置时客户端崩溃的问题，拖拽缩放也不再异常。
- 修复菜单里自定义难度的「+」和「-」按钮重叠、只有一个方向能点的问题。
- 修复临时队伍没有玩家后不会被删除、并不断刷日志的问题。
- 修复服务器在「准备中」阶段崩溃后，重启会永久卡住的问题：现在重启会自动继续这一局。
- 修复「不可获得物品」清单里一处格式错误导致整份清单失效、部分物品被误判的问题。
- 修正了 README 与教程中与实际游戏不符的指令和棋盘说明（例如内置棋盘名称、缺少的新指令）。

#### 兼容性

- 仍为 Minecraft 1.21.1 / NeoForge 21.1.234，存档与配置可直接沿用。
- 与 JEI、Simple Voice Chat、Xaero 小地图、Curios 的联动行为不变，缺失时照旧自动降级。

### English Changelog

#### Added

- **Next round from the results screen**: a "Next Round" button on the results screen, or `/bingo nextround`. It resets the world and starts the next round immediately, without going back to the lobby to ready up (lobby mode only, at least one player must be online).
- **Post-game voting**: while the post-game ready countdown runs, the results screen shows "Next Round" and "Return to Lobby" vote buttons with live vote counts and a check mark on your own choice. When the countdown ends the majority decides; with no votes the lobby is used.
- **World seed command**: `/bingo world seed` shows the terrain seed the next round will use; `/bingo world seed <seed>` sets one and `/bingo world seed random` restores per-round randomization. The setting is kept across restarts, which makes practising the same map easy.
- **A more complete scoreboard**: before a round it lists each team's player count, after a round it lists each team's score, and during the ready countdown it shows "Ready X/Y".
- **An API for other mods**: other mods can now read the current card's tiles, objectives, difficulty tiers and card rules, which makes display and integration mods easier to build.

#### Changed

- `/bingo shuffleteams <count>` now respects the requested team count and spreads players as evenly as possible (7 players over 3 teams become 3/2/2 instead of one player facing three teams).
- HUD clicks now land where you click on high-DPI / Retina screens and with large GUI scales (they used to be offset).
- Offline player data is no longer kept in memory for the lifetime of the server.
- The results screen and the spectator team picker refresh less often, which reduces needless stutter and traffic.
- Round progress is now saved together with the world save, so a crash or forced shutdown loses less progress.

#### Fixed

- **Fixed `/bingo start` hanging when nobody is online**: it used to stop at "starting" with no command able to get out of it; it now tells you to let players join first.
- Fixed `/bingo start` still reporting "starting the game" when it failed.
- Fixed `/ready cancel` restarting the ready countdown immediately instead of actually cancelling it.
- Fixed the ready countdown retrying forever and spamming warnings when the automatic start could not proceed.
- Fixed `/bingo debug` being unavailable in release builds (it only existed in test builds) and crashing when used.
- Fixed `/bingo shuffleteams` crashing when nobody is online, which also wiped the existing teams.
- Fixed `/bingoprefs hide_lobby_prompt` not being toggleable.
- Fixed `/join` creating a team out of nowhere without permission or outside the lobby phase; clicking a team sign in the lobby now also requires the join permission.
- Fixed console or command block use of `/ready`, `/coords`, `/bingo lobby` and similar commands logging errors.
- Fixed server crashes when a card could not be generated: the previous card (or a default one) is now kept and a message is shown.
- Fixed the client crashing when opening the card placement screen in a narrow window, and when dragging to resize the card.
- Fixed the "+" and "-" buttons of the custom difficulty menu overlapping so only one direction worked.
- Fixed temporary teams staying registered after their last player left, which also spammed the log.
- Fixed the server getting permanently stuck when it crashed during the "starting" phase: a restart now continues the round.
- Fixed one malformed entry in the "unobtainable items" list that invalidated the whole list and mis-classified some items.
- Corrected command and board descriptions in the README and tutorials that no longer matched the game (for example built-in board names and missing commands).

#### Compatibility

- Still Minecraft 1.21.1 / NeoForge 21.1.234; saves and configs carry over unchanged.
- JEI, Simple Voice Chat, Xaero's Minimap and Curios integrations behave as before, including graceful fallbacks when they are missing.

——————

## 1.1.3 - 2026-11-25

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Feature update

### 中文更新日志

#### 新增内容

- 生存模式物品栏新增「团队箱子」按钮：就在原版配方书按钮旁边、大小一致，点一下等于输入 `/tc`，不记指令也能开队伍箱。按钮只会在一局进行中、且服务器没有关闭队伍箱时出现；打开配方书把界面推到一边时它会跟着移动。装了 Vanilla Backport 时用铜箱子图标，没装则用原版箱子图标。
- 教程书的「队伍功能」页补充说明：也可以点物品栏里配方书旁边的按钮打开队伍箱。

#### 修复内容

- 修复教程书内容显示不全的问题：以前一页最多显示 14 行，超出的部分会被直接丢掉，导致部分章节被截断；现在会自动分页，可点击的指令链接也保留完整。
- 修复点击「团队箱子」按钮会让客户端崩溃的问题。
- 修复单人游戏里一个存档开过大厅模式后，其他存档跟着变成大厅世界的问题：现在只有通过 BINGO 按钮创建的世界才会装入大厅数据包；客户端启动时还会把普通存档里误装的数据包改名停用（文件保留，需要时改回原名即可），让已经受影响的存档恢复正常。
- 修复专用服务器上给某个存档开启大厅模式后，切换到其他存档也会沿用大厅模式的问题：开启请求现在只对指定的那个存档生效。

#### 调整内容

- 团队箱子按钮的底图换成玩家重绘版，外观与配方书按钮统一。

### English Changelog

#### Added

- A "Team Chest" button in the survival inventory, right next to the vanilla recipe book button and the same size as it. Clicking it is the same as typing `/tc`, so players do not need to remember the command. It only shows during a running round while the team chest is enabled on the server, and it follows the inventory when the recipe book pushes the layout aside. The copper chest icon is used when Vanilla Backport is installed, otherwise the vanilla chest icon is used.

#### Fixed

- Fixed tutorial book pages being cut off: a page rendered at most 14 lines and silently dropped the rest, truncating sections such as "Player Settings". Pages are now split automatically and clickable command links stay intact.
- Fixed a client crash when clicking the "Team Chest" button.
- Fixed lobby mode leaking between singleplayer saves: only worlds created from the BINGO button now install the lobby datapack, and on client startup a stray copy in an ordinary world is disabled by renaming it (the file is kept, rename it back to restore).
- Fixed lobby mode being reused by other saves on a dedicated server: the enable request is now bound to the save it was made for.

#### Changed

- The team chest button uses a player-redrawn background to match the recipe book button.

——————

## 1.1.2 - 2026-11-23

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Bug fix

### 中文更新日志

#### 修复内容

- 修复安装了 Curios 时，每局重置没有真正清空饰品栏的问题（以前会留下一个数量为 0 的物品，而不是把槽位腾空）。现在会真正把饰品取走并清空。

#### 调整内容

- 重新核对了 JEI、Simple Voice Chat、Xaero 小地图、Curios 四个联动在 1.21.1 上的可用性，除上面这条外无需改动。

### English Changelog

#### Fixed

- Fixed accessory slots not actually being cleared on reset when Curios is installed (it used to leave a zero-count item instead of emptying the slot). Slots are now emptied properly.

#### Changed

- Re-checked the JEI, Simple Voice Chat, Xaero's Minimap and Curios integrations on 1.21.1; nothing else needed changing.

——————

## 1.1.1 - 2026-11-22

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Bug fix

### 中文更新日志

#### 调整内容

- 移除了针对 Enigmatic Legacy 的专属处理（大厅自动清理开局遗物等）。只在装了那个模组时才会生效，没装的环境行为完全不变。
- 重新整理 README：明确只与 JEI、Simple Voice Chat、Xaero 小地图、Curios 四个模组联动，以及它们缺失时的表现；删掉已经不存在的联动条目，补全指令表与文档索引。

### English Changelog

#### Changed

- Removed the Enigmatic Legacy specific handling (automatic lobby relic cleanup). It only ran when that mod was installed, so other setups are unaffected.
- Reworked the README to state exactly which four mods are integrated with and how they behave when missing, dropped outdated entries, and completed the command table and documentation index.

——————

## 1.1.0 - 2026-11-22

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Feature update

### 中文更新日志

#### 调整内容

- 移除了所有针对其他模组的专属棋盘（共 12 个，例如灾变、农夫乐事、冰火传说、暮色森林等），现在只内置 4 个原版棋盘：物品、进度、精简、挑战。想用其他模组的内容，可以按教程自建棋盘或用过滤器选入。
- 升级时会自动清理旧版本留下的模组棋盘文件，但只删除玩家没改过的那些，自建或修改过的棋盘会保留。
- 移除了几个实际上早就没生效的联动入口（预生成、占位符、隐身兼容、设置界面等），因此游戏内行为没有变化。
- 同步更新 README 与教程，说明当前只含原版内容，以及如何把其他模组的内容加回来。

#### 修复内容

- 修复启动、`/reload` 或改配置后游戏崩溃的问题。
- 修复配置文件损坏时被直接重置为默认值的问题：现在会先备份坏文件，方便找回。
- 修复对局结束时统计写入失败会让对局永远停在「进行中」的问题。
- 修复计分板模板为空时的报错，以及统计/最佳用时异常导致菜单刷新中断的问题。
- 修复 `/bingodata tag|tierlist` 批量增删通配符时只有最后一项生效的问题。
- 修复多连线目标重复计分、时间限制输入一跳归零、单人最佳用时混进多人记录、旧队伍箱界面点击报错等问题。
- 修复客户端断线后仍发送请求导致崩溃，以及关闭 HUD 界面后残留事件监听的问题。
- 修复旁观者入队界面里写死的中文文案，现在会跟随语言设置显示。

### English Changelog

#### Changed

- Removed every board that targeted another mod (12 in total, e.g. Cataclysm, Farmer's Delight, Ice and Fire, Twilight Forest). The mod now ships four vanilla boards: Items, Advancements, Simplified and Challenge. Other mods' content can be added back with a custom board or a filter, as described in the tutorials.
- Upgrading cleans up old mod board files, but only the ones you never edited; custom or modified boards are preserved.
- Removed several integration entries that no longer did anything (pre-generation, placeholders, vanish compatibility, settings screen), so in-game behaviour is unchanged.
- Updated the README and tutorials to describe the vanilla-only content and how to bring other mods' content back.

#### Fixed

- Fixed the game crashing on startup, `/reload`, or after editing the config.
- Fixed a corrupt config being silently replaced by defaults; the broken file is now backed up first so it can be recovered.
- Fixed a failed stats write leaving the round stuck in progress forever.
- Fixed errors with an empty scoreboard template and menu refreshes breaking when best-time data failed.
- Fixed `/bingodata tag|tierlist` bulk edits only applying the last item.
- Fixed duplicate scoring of shared tiles, the time-limit input jumping straight to zero, singleplayer best times leaking into multiplayer records, and stale team chest menus throwing errors.
- Fixed a client crash from requests sent after disconnecting, and leftover event listeners after closing the HUD screen.
- Fixed hardcoded Chinese text in the spectator team-join screen, which now follows your language setting.

——————

## 1.0.9 - 2026-11-22

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Bug fix

### 中文更新日志

#### 修复内容

- 修复启动、`/reload` 或修改配置后游戏崩溃的问题：此前把模组配置同步到 NeoForge 配置时会反复触发自身重载，最终导致堆栈溢出崩溃。
- 配置同步现在只在数值真的变化时才写回配置文件。

### English Changelog

#### Fixed

- Fixed a crash on startup, `/reload` or after editing the config: syncing the mod config into the NeoForge config used to re-trigger its own reload until the game crashed.
- Config sync now only writes the config file when a value actually changed.

——————

## 1.0.6 - 2026-07-16

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Feature update

### 中文更新日志

#### 新增内容

- 新增长对局性能维护：对局里实体过多时，服务器会定期清理离玩家很远的掉落物、投射物和普通生物，同时保留玩家、坐骑、命名/持久/无敌实体以及受保护的内容。
- 新增 `allowNonOpGameConfiguration` 开关：开启后，非 OP 玩家也能修改游戏配置和用大厅菜单里的设置控件。
- 新增对局中「旁观者申请入队」流程：没有队伍的旁观者可以用 `/teamchest` 打开队伍选择，有人的队伍需要队内玩家点同意，空队伍可以直接加入。
- 新增面向玩家的自定义棋盘教程，讲清楚怎么写棋盘文件、怎么把某个模组的内容加进来。

#### 调整内容

- 优化长局服务器表现：物品计分、地图维护等周期性工作降低频率或改为按事件触发，减少对局中的卡顿，同时计分反馈依然接近即时。
- 对局中所有玩家都下线时，计时、物品判定和胜负判定会自动暂停，避免空服务器把比赛判掉。

#### 修复内容

- 修复活跃对局中关服/重启后可能一边回到大厅状态、一边世界还停在对局地形的问题。
- 修复在开局各阶段关服时，已经开始的对局可能没有被保护、走到错误清理流程的问题。
- 修复更新或迁移配置时可能删掉、覆盖玩家自建棋盘的问题。
- 修复启动时旧配置文件可能覆盖新配置、手动添加的棋盘预设被丢掉的问题。
- 修复自定义棋盘里「只认物品」或「只认进度」的限定丢失、同 ID 目标被合并的问题。
- 修复对局中掉线重连可能误清当前局 Xaero 地图缓存的问题。

### English Changelog

#### Added

- Long-round performance maintenance: when a round has too many entities, the server periodically removes far-away drops, projectiles and regular mobs while keeping players, mounts, named/persistent/invulnerable entities and protected content.
- A new `allowNonOpGameConfiguration` option lets non-OP players change game settings and use the lobby menu controls.
- A spectator team-join flow: teamless spectators can open team selection with `/teamchest`; teams with online members need approval, empty teams can be joined directly.
- A player-facing custom board tutorial explaining board files and how to bring another mod's content in.

#### Changed

- Long rounds run better: item scoring and map upkeep happen less often or on demand, reducing stutter while keeping scoring feedback near instant.
- Timers, item checks and win checks pause while every player is offline, so an empty server does not end the match.

#### Fixed

- Fixed a shutdown/restart during an active round leaving the lobby state while the world was still the round's world.
- Fixed shutdown during the starting phases not being treated as an active round.
- Fixed updates or config migrations deleting or overwriting player-made boards.
- Fixed stale config files overwriting newer ones on startup, and custom board presets being dropped.
- Fixed custom boards losing their "items only" / "advancements only" restrictions and same-ID targets being merged.
- Fixed a reconnect during a round clearing the current round's Xaero map cache.

——————

## 1.0.5 - 2026-07-12

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Feature update

### 中文更新日志

#### 新增内容

- 新增附魔之瓶经验掉落调整：默认每瓶掉落 100–500 点经验，可以在配置里或游戏内配置界面修改。

#### 修复内容

- 修复重置或第二局开局时，跨维度传送偶发失败导致玩家卡在加载中的问题：现在会修正该状态并重新同步客户端。

#### 兼容性

- 新增莱特兰扩充（L2Complements）内置棋盘：装了该模组时，棋盘菜单会多出一个以高阶核心物品为主的目标集。
- 新增 Enigmatic Legacy 大厅兼容处理：前大厅会移除该模组赠送的开局遗物，避免玩家在大厅反复获得这些效果。

### English Changelog

#### Added

- Configurable Bottle o' Enchanting XP: each bottle drops 100–500 XP by default, adjustable in the config or the in-game config screen.

#### Fixed

- Fixed occasional failures during cross-dimension teleports on reset or second-round start leaving players stuck loading; the state is repaired and the client resynced.

#### Compatibility

- Added an L2Complements board that appears when that mod is installed, focused on high-tier core items.
- Added Enigmatic Legacy lobby handling that removes the mod's starter relics in the lobby so they are not repeatedly granted.

——————

## 1.0.4 - 2026-07-07

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Feature update

### 中文更新日志

#### 新增内容

- 新增内置队伍共享箱：同队玩家可以用 `/teamchest` 或 `/tc` 打开 27 格共享箱；装了本模组的客户端还可以按 `B` 打开。
- 新增选项：让队伍箱里的物品也能完成 Bingo 物品格，并在「消耗物品」模式中按需扣除。
- 新增同队传送：`/teamtp <玩家>` 或 `/ttp <玩家>`。
- 新增队伍功能开关与开局前切换指令：`/teamchest toggle`、`/teamchest count`、`/tptoggle`。
- 新增按棋盘来源调整抽取权重的配置，把某个来源设为 0 即可让它不参与随机抽取。

#### 调整内容

- 连服务器时，NeoForge 配置界面可以读取并（对有权限的玩家）修改服务端设置。
- 大厅教程书补充队伍箱与队伍传送说明，并加了可点击的指令。
- 调整若干模组内容棋盘的难度分级，把颜色变体合并成「任意一种」，不再强制某个具体颜色。

#### 修复内容

- 修复内存紧张或区块加载慢时，开局后地形出来了但实体不显示、破坏方块回弹或看到虚空、必须重进才恢复的问题。
- 修复默认难度预设被写回后顺序错乱、导致难度菜单缺少选项的问题。
- 修复专用服务器上收纳袋配方被旧配方覆盖的问题。

#### 兼容性

- 修复安装 Simple Voice Chat 时，语音连接重建期间渲染大厅交互实体导致客户端崩溃的问题。

### English Changelog

#### Added

- Built-in team shared chests: teammates can open a 27-slot chest with `/teamchest` or `/tc`, or with the `B` key on clients that have ExBingo installed.
- An option to let team chest items count towards Bingo item tiles, consumed as needed in consume-items mode.
- Same-team teleporting via `/teamtp <player>` or `/ttp <player>`.
- Team feature toggles and pre-game commands: `/teamchest toggle`, `/teamchest count`, `/tptoggle`.
- A config to weight card generation by board source; setting a source to 0 keeps it out of random picks.

#### Changed

- The NeoForge config screen now reads the server's settings when connected, and lets permitted players edit them.
- The lobby tutorial book now explains the team chest and team teleport, with clickable commands.
- Rebalanced several mod-content boards and merged colour variants into "any of" objectives instead of requiring one specific colour.

#### Fixed

- Fixed starts under memory pressure or slow chunk loading where entities did not render, blocks rubber-banded or void showed, requiring a reconnect.
- Fixed default difficulty presets being reordered, which hid options in the difficulty menu.
- Fixed the bundle recipe being overridden by an old recipe on dedicated servers.

#### Compatibility

- Fixed a client crash in Simple Voice Chat when lobby interaction entities rendered during a voice connection reset.

——————

## 1.0.3 - 2026-07-07

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Feature update

### 中文更新日志

#### 新增内容

- 新增游戏内配置界面入口，可以直接在游戏里调整常用选项，同时仍然写回原来的配置文件。
- 新增可选的物品与进度难度标记：开启后，棋盘、得分提示和进度界面会显示 `S/A/B/C/D` 难度字母。
- 增强 Xaero 小地图联动：对局中同步同队玩家位置；队友分享的路径点会作为队内消息。

#### 调整内容

- 难度标记改用目标自己的难度，不再借用展示物品的难度。
- 优化棋盘 HUD 与地图同步，减少没有变化时的无谓刷新。
- 调整若干模组棋盘的难度分级。

#### 修复内容

- 修复服务器在准备阶段重启时，手动改过的对局选项被旧存档覆盖的问题。
- 修复刚进服或重置后卡片只显示空框、25 个格子内容不出现的问题。
- 修复开局加载地形时误用上一次的加载信号，导致玩家过早进入世界、卡虚空或无法破坏方块的问题。
- 修复重置世界后新世界没被服务器接管，导致回到大厅后实体冻结、方块交互异常的问题。
- 修复大厅左键点方块可能崩溃的问题。
- 修复重掷、改格、换卡等操作后 HUD 仍显示旧棋盘的问题。
- 修复用 JEI 收藏卡片目标时可能收藏异常的问题。
- 修复专用服务器上收纳袋相关内容（配方等）不可用的问题。

#### 兼容性

- 难度标记同时支持原版进度界面和 Better Advancements；联机时以服务器数据为准。

### English Changelog

#### Added

- An in-game config screen entry for common options, still written back to the usual config file.
- Optional item and advancement difficulty markers: cards, score messages and progression screens can show `S/A/B/C/D`.
- Better Xaero integration: teammate positions are synced during rounds, and teammates' shared waypoints are delivered as team messages.

#### Changed

- Difficulty markers now use the objective's own tier rather than the display item's tier.
- Reduced pointless card HUD and map refreshes when nothing changed.
- Rebalanced tiers on several mod-content boards.

#### Fixed

- Fixed manually edited round options being overwritten by stale saved options when the server restarted in the lobby phase.
- Fixed an empty card frame right after joining or a reset, with tiles missing until something else refreshed.
- Fixed terrain loading reusing stale signals, which could put players into the world too early (void, unbreakable blocks).
- Fixed recreated worlds not being picked up after a reset, causing frozen entities and broken block interaction.
- Fixed a crash when left-clicking lobby blocks.
- Fixed old card contents lingering on the HUD after rerolls, tile edits and card changes.
- Fixed JEI bookmarking of card targets misbehaving.
- Fixed bundle-related content being unavailable on dedicated servers.

#### Compatibility

- Difficulty markers work on both the vanilla advancements screen and Better Advancements; in multiplayer the server's data is authoritative.

——————

## 1.0.2 - 2026-07-04

- Minecraft: 1.21.1
- Mod Loader: NeoForge 21.1.234
- License: LGPL-3.0-only
- Release Type: Feature update

### 中文更新日志

#### 新增内容

- 新增在棋盘界面直接收藏物品的功能，方便标记想收集的目标。

#### 调整内容

- 调整暮色森林棋盘的物品分级，移除部分不适合作为收集目标的条目。

#### 修复内容

- 修复多人开局时网络较慢的玩家在地形加载完成前就进入世界、卡虚空或无法破坏方块的问题；现在会等每位玩家的出生地形加载完再开始。

### English Changelog

#### Added

- You can now favourite items directly from the card screen to mark targets you care about.

#### Changed

- Adjusted tiers on the Twilight Forest board and removed entries that do not work as collection targets.

#### Fixed

- Fixed multiplayer starts dropping high-latency players into the world before their terrain loaded, leaving them stuck in the void or unable to break blocks; the game now waits for every player's spawn terrain.
