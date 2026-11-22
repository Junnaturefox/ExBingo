# 模组内容教程

本目录面向**想往棋盘里加入其他模组内容**的玩家与服主。

## 先读：当前版本的棋盘现状

从移除模组联动棋盘之后，ExBingo **只内置 4 个原版棋盘**：

| 棋盘 ID | 名称 |
|---|---|
| `items` | 物品 |
| `in_overworld` | 主世界 |
| `advancements` | 进度 |
| `simplified` | 精简 |

过去随模组附带的 12 个模组棋盘（`cataclysm` 灾变、`eternal_starlight` 永恒星光、`farmersdelight` 农夫乐事、`fdbosses` 逆卡巴拉：觉醒、`iceandfire` 冰火传说、`irons_spellbooks` Iron 的法术与魔法书、`l2complements` 莱特兰扩充、`mowziesmobs` Mowzie 的生物、`netherman`、`the_bumblezone` 蜜蜂领域、`twilightforest` 暮色森林、`undergarden` 深暗之园）以及 `data/iceandfire/` 的专属目标**已全部移除**。

因此：

- 装了其他模组后，它们的物品/进度**默认不会**出现在棋盘上。
- 想把它们加回棋盘，就是本目录两份教程要解决的问题。

## 两种做法

| 做法 | 适用场景 | 精度 | 入口 |
|---|---|---|---|
| **按命名空间引入** `+from=<模组id>` | 想直接把某个模组的已分类内容纳入 | 宽泛，可能是整个模组 | [Bingo指令教程](Bingo指令教程.md) 的「棋盘筛选与难度」 |
| **自定义棋盘**：写 tierlist + 在 `itemFilterPresets` 里加一行 | 想精确指定每一个目标、控制难度档位 | 精确，逐条可控 | [自定义棋盘教程](自定义棋盘教程.md) |
| **自动分级** `/bingo autotier generate` | 模组内容很多，懒得逐个分类 | 由模组按获取难度自动推断 | [Bingo指令教程](Bingo指令教程.md) 的「统计与数据维护」 |

三者也刻意组合：先用 `/bingo autotier generate` 自动分级，再把生成的 `autotier.tierlist.json` 抄成自己的自定义棋盘微调。

## 文件索引

- [自定义棋盘教程.md](自定义棋盘教程.md) —— 用 tierlist 精确指定棋盘内容，并让它出现在游戏内棋盘列表里。
- [Bingo指令教程.md](Bingo指令教程.md) —— 全部常用指令，含筛选表达式、难度设置、数据维护与统计。

## 相关配置与默认行为

改动棋盘内容时，这两个配置会影响结果（`config/exbingo/config.json`）：

- `excludeModUncategorizedFromCards`（默认 `true`）—— 未分类的模组内容默认不进棋盘。用 `+from=<模组id>` 时建议同时加 `-uncategorized`，只留该模组已分类的内容；反之想全都要就去掉它。
- `excludeUnbreakableBlocksFromCards`（默认 `true`）—— 不可破坏方块（基岩、屏障、命令方块等）默认不进棋盘。

配置改完记得 `/reload` 或重启；棋盘列表有缓存，重载后才能看到新增/关闭变化。
