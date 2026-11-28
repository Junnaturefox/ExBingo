package me.jfenn.bingo.common.config

import me.jfenn.bingo.common.card.autotier.AutoTierConfig
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.utils.json
import net.neoforged.neoforge.common.ModConfigSpec
import java.util.UUID
import java.util.function.Predicate
import java.util.function.Supplier

class CommonValues(builder: ModConfigSpec.Builder) {
    val entries = mutableListOf<ConfigEntry>()
    val sections = mutableListOf<ConfigSection>()

    init {
        builder.pushSection("cards", "Board and card-generation defaults.", commonSectionKey("cards"), sections)
    }

    val disabledFilterPresets = builder.defineStringList(
        "disabledFilterPresets",
        commonKey("cards", "disabled_filter_presets"),
        "Board preset ids hidden from board selection.",
        "disabledFilterPresets",
        { defaultConfig().disabledFilterPresets.toList().sorted() },
        entries,
        allowBlank = false,
    )
    val itemFilterPresets = builder.defineStringList(
        "itemFilterPresets",
        commonKey("cards", "item_filter_presets"),
        "Board preset definitions written as preset_id=filter expression.",
        "itemFilterPresets",
        { itemFilterPresetList(defaultConfig().itemFilterPresets) },
        entries,
        allowBlank = false,
        validator = ::isItemFilterPresetEntry,
    )
    val difficultyPresets = builder.defineStringList(
        "difficultyPresets",
        commonKey("cards", "difficulty_presets"),
        "Card difficulty distributions written as preset_id=s,a,b,c,d. The five counts must add up to 25.",
        "difficultyPresets",
        { difficultyPresetList(defaultConfig().difficultyPresets) },
        entries,
        allowBlank = false,
        validator = ::isDifficultyPresetEntry,
    )
    val boardSourceWeights = builder.defineStringList(
        "boardSourceWeights",
        commonKey("cards", "board_source_weights"),
        "Card source weights written as source_id=weight. Sources not listed use 1.0; 0 disables a source.",
        "boardSourceWeights",
        { boardSourceWeightList(defaultConfig().boardSourceWeights) },
        entries,
        allowBlank = false,
        validator = ::isBoardSourceWeightEntry,
    )
    val preventScoringSpawnKitItems = builder.defineBoolean(
        "preventScoringSpawnKitItems",
        commonKey("cards", "prevent_scoring_spawn_kit_items"),
        "Adds bingo_ignore to spawn kit items so they cannot score card tiles.",
        "preventScoringSpawnKitItems",
        { defaultConfig().preventScoringSpawnKitItems },
        entries,
    )
    val excludeSpawnKitItemsFromCards = builder.defineBoolean(
        "excludeSpawnKitItemsFromCards",
        commonKey("cards", "exclude_spawn_kit_items_from_cards"),
        "Removes active spawn kit items from generated cards.",
        "excludeSpawnKitItemsFromCards",
        { defaultConfig().excludeSpawnKitItemsFromCards },
        entries,
    )
    val excludeModUncategorizedFromCards = builder.defineBoolean(
        "excludeModUncategorizedFromCards",
        commonKey("cards", "exclude_mod_uncategorized_from_cards"),
        "Keeps uncategorized non-vanilla objectives off cards unless a filter selects them explicitly.",
        "excludeModUncategorizedFromCards",
        { defaultConfig().excludeModUncategorizedFromCards },
        entries,
    )
    val excludeUnbreakableBlocksFromCards = builder.defineBoolean(
        "excludeUnbreakableBlocksFromCards",
        commonKey("cards", "exclude_unbreakable_blocks_from_cards"),
        "Keeps objectives for unbreakable placed blocks off cards.",
        "excludeUnbreakableBlocksFromCards",
        { defaultConfig().excludeUnbreakableBlocksFromCards },
        entries,
    )

    init {
        builder.pop()
        builder.pushSection("autoTier", "Automatic tiering defaults for uncategorized modded objectives.", commonSectionKey("auto_tier"), sections)
    }

    val autoTierName = builder.defineString(
        "autoTier.tierListName",
        commonKey("auto_tier", "tier_list_name"),
        "Tier list file name written by /bingo autotier generate.",
        "tierListName",
        { defaultConfig().autoTier.tierListName },
        entries,
        allowBlank = false,
    )
    val autoTierMapping = builder.defineEnum(
        "autoTier.mapping",
        commonKey("auto_tier", "mapping"),
        "Controls how auto-tier scores are converted to S/A/B/C/D tiers.",
        "mapping",
        { defaultConfig().autoTier.mapping },
        entries,
    )
    val autoTierThresholds = builder.defineDoubleList(
        "autoTier.thresholds",
        commonKey("auto_tier", "thresholds"),
        "Absolute score cutoffs for S, A, B, C, and D when mapping is THRESHOLD.",
        "thresholds",
        { defaultConfig().autoTier.thresholds },
        entries,
        min = 0.0,
        max = 100000.0,
    )
    val autoTierRecipeStepCost = builder.defineDouble(
        "autoTier.recipeStepCost",
        commonKey("auto_tier", "recipe_step_cost"),
        "Score added for each recipe step while auto-tiering.",
        "recipeStepCost",
        { defaultConfig().autoTier.recipeStepCost },
        entries,
        min = 0.0,
        max = 100000.0,
    )

    init {
        builder.pop()
        builder.pushSection("lobby", "Lobby, tutorial, and postgame behavior.", commonSectionKey("lobby"), sections)
    }

    val preventLobbyChaos = builder.defineBoolean(
        "preventLobbyChaos",
        commonKey("lobby", "prevent_lobby_chaos"),
        "Stops lobby sounds from being sent to other players.",
        "preventLobbyChaos",
        { defaultConfig().preventLobbyChaos },
        entries,
    )
    val lobbyTutorialBook = builder.defineBoolean(
        "lobbyTutorialBook",
        commonKey("lobby", "lobby_tutorial_book"),
        "Gives players a tutorial/settings book when they join the lobby.",
        "lobbyTutorialBook",
        { defaultConfig().lobbyTutorialBook },
        entries,
    )
    val giveMementoInSurvival = builder.defineBoolean(
        "giveMementoInSurvival",
        commonKey("lobby", "give_memento_in_survival"),
        "Gives players a memento card after a survival-mode game ends.",
        "giveMementoInSurvival",
        { defaultConfig().giveMementoInSurvival },
        entries,
    )
    val nightVisionInSpectator = builder.defineBoolean(
        "nightVisionInSpectator",
        commonKey("lobby", "night_vision_in_spectator"),
        "Grants night vision while spectating when player settings allow it.",
        "nightVisionInSpectator",
        { defaultConfig().nightVisionInSpectator },
        entries,
    )
    val nightVisionInPostgame = builder.defineBoolean(
        "nightVisionInPostgame",
        commonKey("lobby", "night_vision_in_postgame"),
        "Grants night vision after a game ends when player settings allow it.",
        "nightVisionInPostgame",
        { defaultConfig().nightVisionInPostgame },
        entries,
    )
    val revealAllAdvancements = builder.defineBoolean(
        "revealAllAdvancements",
        commonKey("lobby", "reveal_all_advancements"),
        "Shows the whole vanilla advancement tree from the start.",
        "revealAllAdvancements",
        { defaultConfig().revealAllAdvancements },
        entries,
    )
    val supportClientHud = builder.defineBoolean(
        "supportClientHud",
        commonKey("lobby", "support_client_hud"),
        "Allows connecting clients to use the Bingo HUD instead of only map/card behavior.",
        "supportClientHud",
        { defaultConfig().supportClientHud },
        entries,
    )
    val allowNonOpGameConfiguration = builder.defineBoolean(
        "allowNonOpGameConfiguration",
        commonKey("lobby", "allow_non_op_game_configuration"),
        "Allows non-operator players to use game configuration commands and lobby menu controls.",
        "allowNonOpGameConfiguration",
        { defaultConfig().allowNonOpGameConfiguration },
        entries,
    )

    init {
        builder.pop()
        builder.pushSection("team", "Team chest and teleport behavior.", commonSectionKey("team"), sections)
    }

    val teamChestEnabled = builder.defineBoolean(
        "teamChestEnabled",
        commonKey("team", "team_chest_enabled"),
        "Enables each Bingo team to use a persistent shared chest.",
        "teamChestEnabled",
        { defaultConfig().teamChestEnabled },
        entries,
    )
    val teamChestCountsForObjectives = builder.defineBoolean(
        "teamChestCountsForObjectives",
        commonKey("team", "team_chest_counts_for_objectives"),
        "Allows matching team chest items to score Bingo item objectives and be consumed in consume-items mode.",
        "teamChestCountsForObjectives",
        { defaultConfig().teamChestCountsForObjectives },
        entries,
    )
    val teamTeleportEnabled = builder.defineBoolean(
        "teamTeleportEnabled",
        commonKey("team", "team_teleport_enabled"),
        "Allows players to teleport to teammates.",
        "teamTeleportEnabled",
        { defaultConfig().teamTeleportEnabled },
        entries,
    )

    init {
        builder.pop()
        builder.pushSection("gameplay", "Vanilla gameplay tweaks.", commonSectionKey("gameplay"), sections)
    }

    val experienceBottleXpEnabled = builder.defineBoolean(
        "experienceBottleXp.enabled",
        commonKey("gameplay", "experience_bottle_xp_enabled"),
        "Overrides Bottle o' Enchanting XP drops with a configurable random range.",
        "experienceBottleXp.enabled",
        { defaultConfig().experienceBottleXp.enabled },
        entries,
    )
    val experienceBottleXpMin = builder.defineInt(
        "experienceBottleXp.min",
        commonKey("gameplay", "experience_bottle_xp_min"),
        "Minimum XP awarded by one Bottle o' Enchanting when the override is enabled.",
        "experienceBottleXp.min",
        { defaultConfig().experienceBottleXp.min },
        entries,
        min = 0,
        max = 100000,
    )
    val experienceBottleXpMax = builder.defineInt(
        "experienceBottleXp.max",
        commonKey("gameplay", "experience_bottle_xp_max"),
        "Maximum XP awarded by one Bottle o' Enchanting when the override is enabled.",
        "experienceBottleXp.max",
        { defaultConfig().experienceBottleXp.max },
        entries,
        min = 0,
        max = 100000,
    )

    init {
        builder.pop()
        builder.pushSection("flow", "Ready timers, countdowns, and reset behavior.", commonSectionKey("flow"), sections)
    }

    val startWhenReadySeconds = builder.defineInt(
        "startWhenReadySeconds",
        commonKey("flow", "start_when_ready_seconds"),
        "Seconds before the game starts automatically; -1 disables this timer.",
        "startWhenReadySeconds",
        { defaultConfig().startWhenReadySeconds ?: DISABLED_SECONDS },
        entries,
        min = DISABLED_SECONDS,
        max = 86400,
    )
    val startWhenReadyWaitsForTeams = builder.defineBoolean(
        "startWhenReadyWaitsForTeams",
        commonKey("flow", "start_when_ready_waits_for_teams"),
        "Prevents the ready timer from starting until teams are available.",
        "startWhenReadyWaitsForTeams",
        { defaultConfig().startWhenReadyWaitsForTeams },
        entries,
    )
    val startWhenReadyWaitsForFirstVote = builder.defineBoolean(
        "startWhenReadyWaitsForFirstVote",
        commonKey("flow", "start_when_ready_waits_for_first_vote"),
        "Prevents the ready timer from starting until someone votes ready.",
        "startWhenReadyWaitsForFirstVote",
        { defaultConfig().startWhenReadyWaitsForFirstVote },
        entries,
    )
    val nextRoundWhenReadySeconds = builder.defineInt(
        "nextRoundWhenReadySeconds",
        commonKey("flow", "next_round_when_ready_seconds"),
        "Seconds before the next round starts automatically; -1 disables this timer.",
        "nextRoundWhenReadySeconds",
        { defaultConfig().nextRoundWhenReadySeconds ?: DISABLED_SECONDS },
        entries,
        min = DISABLED_SECONDS,
        max = 86400,
    )
    val nextRoundWhenReadyWaitsForFirstVote = builder.defineBoolean(
        "nextRoundWhenReadyWaitsForFirstVote",
        commonKey("flow", "next_round_when_ready_waits_for_first_vote"),
        "Prevents the next-round ready timer from starting until someone votes ready.",
        "nextRoundWhenReadyWaitsForFirstVote",
        { defaultConfig().nextRoundWhenReadyWaitsForFirstVote },
        entries,
    )
    val countdownDelayTicks = builder.defineInt(
        "countdownDelayTicks",
        commonKey("flow", "countdown_delay_ticks"),
        "Ticks to wait before the start countdown begins.",
        "countdownDelayTicks",
        { defaultConfig().countdownDelayTicks },
        entries,
        min = 0,
        max = 72000,
    )
    val countdownSeconds = builder.defineInt(
        "countdownSeconds",
        commonKey("flow", "countdown_seconds"),
        "Visible countdown length in seconds.",
        "countdownSeconds",
        { defaultConfig().countdownSeconds },
        entries,
        min = 0,
        max = 3600,
    )
    val nextRoundWhenEveryoneDisconnects = builder.defineBoolean(
        "nextRoundWhenEveryoneDisconnects",
        commonKey("flow", "next_round_when_everyone_disconnects"),
        "Starts the next round or reset flow after all players disconnect in postgame.",
        "nextRoundWhenEveryoneDisconnects",
        { defaultConfig().nextRoundWhenEveryoneDisconnects },
        entries,
    )
    val unsafeSkipWorldClose = builder.defineBoolean(
        "unsafeSkipWorldClose",
        commonKey("flow", "unsafe_skip_world_close"),
        "Skips part of world close during reset. This is faster but can risk bugs or data corruption.",
        "unsafeSkipWorldClose",
        { defaultConfig().unsafeSkipWorldClose },
        entries,
    )

    init {
        builder.pop()
        builder.pushSection("chat", "Team chat and command alias defaults.", commonSectionKey("chat"), sections)
    }

    val chatDefaultToTeamChat = builder.defineBoolean(
        "chat.defaultToTeamChat",
        commonKey("chat", "default_to_team_chat"),
        "New players use team chat by default when they are on a team.",
        "defaultToTeamChat",
        { defaultConfig().chat.defaultToTeamChat },
        entries,
    )
    val chatDefaultToSpectatorChat = builder.defineBoolean(
        "chat.defaultToSpectatorChat",
        commonKey("chat", "default_to_spectator_chat"),
        "Spectators use spectator chat by default.",
        "defaultToSpectatorChat",
        { defaultConfig().chat.defaultToSpectatorChat },
        entries,
    )
    val chatGlobalCommandAliases = builder.defineStringList(
        "chat.globalCommandAliases",
        commonKey("chat", "global_command_aliases"),
        "Command aliases that send messages to global chat.",
        "globalCommandAliases",
        { defaultConfig().chat.globalCommandAliases },
        entries,
        allowBlank = false,
    )
    val chatTeamCommandAliases = builder.defineStringList(
        "chat.teamCommandAliases",
        commonKey("chat", "team_command_aliases"),
        "Command aliases that send messages to team chat.",
        "teamCommandAliases",
        { defaultConfig().chat.teamCommandAliases },
        entries,
        allowBlank = false,
    )

    init {
        builder.pop()
        builder.pushSection("stats", "Stats storage and sync settings.", commonSectionKey("stats"), sections)
    }

    val databaseUrl = builder.defineString(
        "databaseUrl",
        commonKey("stats", "database_url"),
        "Optional JDBC database URL. Leave blank to use the bundled SQLite database.",
        "databaseUrl",
        { defaultConfig().databaseUrl.orEmpty() },
        entries,
        allowBlank = true,
    )
    val databaseUser = builder.defineString(
        "databaseUser",
        commonKey("stats", "database_user"),
        "Optional database username.",
        "databaseUser",
        { defaultConfig().databaseUser.orEmpty() },
        entries,
        allowBlank = true,
    )
    val databasePass = builder.defineString(
        "databasePass",
        commonKey("stats", "database_pass"),
        "Optional database password. This is stored as plain text in the config file.",
        "databasePass",
        { defaultConfig().databasePass.orEmpty() },
        entries,
        allowBlank = true,
    )
    val syncStats = builder.defineBoolean(
        "syncStats",
        commonKey("stats", "sync_stats"),
        "Syncs Bingo statistics with compatible servers and clients.",
        "syncStats",
        { defaultConfig().syncStats },
        entries,
    )
    val statsHostId = builder.defineString(
        "statsHostId",
        commonKey("stats", "stats_host_id"),
        "Unique host id used to distinguish locally played games from synced server games.",
        "statsHostId",
        { defaultConfig().statsHostId.toString() },
        entries,
        allowBlank = false,
        validator = { runCatching { UUID.fromString(it) }.isSuccess },
    )

    init {
        builder.pop()
        builder.pushSection("server", "Dedicated-server and reset-file behavior.", commonSectionKey("server"), sections)
    }

    val serverIsLobbyMode = builder.defineBoolean(
        "server.isLobbyMode",
        commonKey("server", "is_lobby_mode"),
        "Enables lobby mode. In lobby mode ExBingo can delete and recreate world data after games.",
        "isLobbyMode",
        { defaultConfig().server.isLobbyMode },
        entries,
    )
    val serverPreloadViewDistance = builder.defineInt(
        "server.preloadViewDistance",
        commonKey("server", "preload_view_distance"),
        "Chunk view distance used while preloading team spawn areas.",
        "preloadViewDistance",
        { defaultConfig().server.preloadViewDistance },
        entries,
        min = 0,
        max = 32,
    )
    val serverFilesToReset = builder.defineStringList(
        "server.filesToReset",
        commonKey("server", "files_to_reset"),
        "World-save paths or globs removed when ExBingo resets the world.",
        "filesToReset",
        { defaultConfig().server.filesToReset },
        entries,
        allowBlank = false,
    )

    init {
        builder.pop()
        builder.pushSection("defaultPlayerSettings", "Defaults for newly created per-player settings.", commonSectionKey("default_player_settings"), sections)
    }

    val defaultSeenTutorial = builder.defineBoolean(
        "server.defaultPlayerSettings.seenTutorial",
        commonKey("default_player_settings", "seen_tutorial"),
        "Default value for whether a player has seen the intro tutorial.",
        "seenTutorial",
        { defaultConfig().server.defaultPlayerSettings.seenTutorial },
        entries,
    )
    val defaultHideLobbyPrompt = builder.defineBoolean(
        "server.defaultPlayerSettings.hideLobbyPrompt",
        commonKey("default_player_settings", "hide_lobby_prompt"),
        "Default value for hiding the lobby prompt.",
        "hideLobbyPrompt",
        { defaultConfig().server.defaultPlayerSettings.hideLobbyPrompt },
        entries,
    )
    val defaultBossbar = builder.defineBoolean(
        "server.defaultPlayerSettings.bossbar",
        commonKey("default_player_settings", "bossbar"),
        "Default value for showing game information in a bossbar.",
        "bossbar",
        { defaultConfig().server.defaultPlayerSettings.bossbar },
        entries,
    )
    val defaultScoreboard = builder.defineBoolean(
        "server.defaultPlayerSettings.scoreboard",
        commonKey("default_player_settings", "scoreboard"),
        "Default value for showing game information in a scoreboard.",
        "scoreboard",
        { defaultConfig().server.defaultPlayerSettings.scoreboard },
        entries,
    )
    val defaultScoreboardAutoHide = builder.defineBoolean(
        "server.defaultPlayerSettings.scoreboardAutoHide",
        commonKey("default_player_settings", "scoreboard_auto_hide"),
        "Default value for hiding the scoreboard when the player is not holding a Bingo card.",
        "scoreboardAutoHide",
        { defaultConfig().server.defaultPlayerSettings.scoreboardAutoHide },
        entries,
    )
    val defaultLeadingMessages = builder.defineBoolean(
        "server.defaultPlayerSettings.leadingMessages",
        commonKey("default_player_settings", "leading_messages"),
        "Default value for floating messages when the leading team changes.",
        "leadingMessages",
        { defaultConfig().server.defaultPlayerSettings.leadingMessages },
        entries,
    )
    val defaultScoreMessages = builder.defineBoolean(
        "server.defaultPlayerSettings.scoreMessages",
        commonKey("default_player_settings", "score_messages"),
        "Default value for floating messages when lines or cards are scored.",
        "scoreMessages",
        { defaultConfig().server.defaultPlayerSettings.scoreMessages },
        entries,
    )
    val defaultItemMessages = builder.defineBoolean(
        "server.defaultPlayerSettings.itemMessages",
        commonKey("default_player_settings", "item_messages"),
        "Default value for floating messages when items are scored.",
        "itemMessages",
        { defaultConfig().server.defaultPlayerSettings.itemMessages },
        entries,
    )
    val defaultNightVision = builder.defineBoolean(
        "server.defaultPlayerSettings.nightVision",
        commonKey("default_player_settings", "night_vision"),
        "Default value for opt-in night vision.",
        "nightVision",
        { defaultConfig().server.defaultPlayerSettings.nightVision },
        entries,
    )

    init {
        builder.pop()
    }

    fun applyTo(config: BingoConfig): BingoConfig =
        config.copy(
            itemFilterPresets = mergeItemFilterPresets(config.itemFilterPresets, itemFilterPresets.get()),
            difficultyPresets = parseDifficultyPresetList(difficultyPresets.get()),
            boardSourceWeights = parseBoardSourceWeightList(boardSourceWeights.get()),
            disabledFilterPresets = disabledFilterPresets.get().toSet(),
            preventScoringSpawnKitItems = preventScoringSpawnKitItems.get(),
            excludeSpawnKitItemsFromCards = excludeSpawnKitItemsFromCards.get(),
            excludeModUncategorizedFromCards = excludeModUncategorizedFromCards.get(),
            excludeUnbreakableBlocksFromCards = excludeUnbreakableBlocksFromCards.get(),
            autoTier = AutoTierConfig(
                tierListName = autoTierName.get(),
                mapping = autoTierMapping.get(),
                thresholds = autoTierThresholds.get().toList(),
                recipeStepCost = autoTierRecipeStepCost.get(),
            ),
            preventLobbyChaos = preventLobbyChaos.get(),
            lobbyTutorialBook = lobbyTutorialBook.get(),
            giveMementoInSurvival = giveMementoInSurvival.get(),
            nightVisionInSpectator = nightVisionInSpectator.get(),
            nightVisionInPostgame = nightVisionInPostgame.get(),
            revealAllAdvancements = revealAllAdvancements.get(),
            supportClientHud = supportClientHud.get(),
            allowNonOpGameConfiguration = allowNonOpGameConfiguration.get(),
            teamChestEnabled = teamChestEnabled.get(),
            teamChestCountsForObjectives = teamChestCountsForObjectives.get(),
            teamTeleportEnabled = teamTeleportEnabled.get(),
            experienceBottleXp = ExperienceBottleXpConfig(
                enabled = experienceBottleXpEnabled.get(),
                min = experienceBottleXpMin.get(),
                max = experienceBottleXpMax.get(),
            ),
            startWhenReadySeconds = startWhenReadySeconds.get().takeUnless { it == DISABLED_SECONDS },
            startWhenReadyWaitsForTeams = startWhenReadyWaitsForTeams.get(),
            startWhenReadyWaitsForFirstVote = startWhenReadyWaitsForFirstVote.get(),
            nextRoundWhenReadySeconds = nextRoundWhenReadySeconds.get().takeUnless { it == DISABLED_SECONDS },
            nextRoundWhenReadyWaitsForFirstVote = nextRoundWhenReadyWaitsForFirstVote.get(),
            countdownDelayTicks = countdownDelayTicks.get(),
            countdownSeconds = countdownSeconds.get(),
            nextRoundWhenEveryoneDisconnects = nextRoundWhenEveryoneDisconnects.get(),
            unsafeSkipWorldClose = unsafeSkipWorldClose.get(),
            chat = ChatConfig(
                defaultToTeamChat = chatDefaultToTeamChat.get(),
                defaultToSpectatorChat = chatDefaultToSpectatorChat.get(),
                globalCommandAliases = chatGlobalCommandAliases.get().toList(),
                teamCommandAliases = chatTeamCommandAliases.get().toList(),
            ),
            databaseUrl = databaseUrl.get().blankToNull(),
            databaseUser = databaseUser.get().blankToNull(),
            databasePass = databasePass.get().blankToNull(),
            syncStats = syncStats.get(),
            statsHostId = UUID.fromString(statsHostId.get()),
            server = ServerConfig(
                isLobbyMode = serverIsLobbyMode.get(),
                // JSON-only field: the TOML has no entry for it, so it must be carried over
                // explicitly or applying the spec would drop the save binding.
                isLobbyModeWorld = config.server.isLobbyModeWorld,
                preloadViewDistance = serverPreloadViewDistance.get(),
                filesToReset = serverFilesToReset.get().toList(),
                defaultPlayerSettings = PlayerSettings(
                    seenTutorial = defaultSeenTutorial.get(),
                    hideLobbyPrompt = defaultHideLobbyPrompt.get(),
                    bossbar = defaultBossbar.get(),
                    scoreboard = defaultScoreboard.get(),
                    scoreboardAutoHide = defaultScoreboardAutoHide.get(),
                    leadingMessages = defaultLeadingMessages.get(),
                    scoreMessages = defaultScoreMessages.get(),
                    itemMessages = defaultItemMessages.get(),
                    nightVision = defaultNightVision.get(),
                ),
            ),
        )

    fun setFrom(config: BingoConfig) {
        itemFilterPresets.set(itemFilterPresetList(config.itemFilterPresets))
        difficultyPresets.set(difficultyPresetList(config.difficultyPresets))
        boardSourceWeights.set(boardSourceWeightList(config.boardSourceWeights))
        disabledFilterPresets.set(config.disabledFilterPresets.toList().sorted())
        preventScoringSpawnKitItems.set(config.preventScoringSpawnKitItems)
        excludeSpawnKitItemsFromCards.set(config.excludeSpawnKitItemsFromCards)
        excludeModUncategorizedFromCards.set(config.excludeModUncategorizedFromCards)
        excludeUnbreakableBlocksFromCards.set(config.excludeUnbreakableBlocksFromCards)
        autoTierName.set(config.autoTier.tierListName)
        autoTierMapping.set(config.autoTier.mapping)
        autoTierThresholds.set(config.autoTier.thresholds)
        autoTierRecipeStepCost.set(config.autoTier.recipeStepCost)
        preventLobbyChaos.set(config.preventLobbyChaos)
        lobbyTutorialBook.set(config.lobbyTutorialBook)
        giveMementoInSurvival.set(config.giveMementoInSurvival)
        nightVisionInSpectator.set(config.nightVisionInSpectator)
        nightVisionInPostgame.set(config.nightVisionInPostgame)
        revealAllAdvancements.set(config.revealAllAdvancements)
        supportClientHud.set(config.supportClientHud)
        allowNonOpGameConfiguration.set(config.allowNonOpGameConfiguration)
        teamChestEnabled.set(config.teamChestEnabled)
        teamChestCountsForObjectives.set(config.teamChestCountsForObjectives)
        teamTeleportEnabled.set(config.teamTeleportEnabled)
        experienceBottleXpEnabled.set(config.experienceBottleXp.enabled)
        experienceBottleXpMin.set(config.experienceBottleXp.min)
        experienceBottleXpMax.set(config.experienceBottleXp.max)
        startWhenReadySeconds.set(config.startWhenReadySeconds ?: DISABLED_SECONDS)
        startWhenReadyWaitsForTeams.set(config.startWhenReadyWaitsForTeams)
        startWhenReadyWaitsForFirstVote.set(config.startWhenReadyWaitsForFirstVote)
        nextRoundWhenReadySeconds.set(config.nextRoundWhenReadySeconds ?: DISABLED_SECONDS)
        nextRoundWhenReadyWaitsForFirstVote.set(config.nextRoundWhenReadyWaitsForFirstVote)
        countdownDelayTicks.set(config.countdownDelayTicks)
        countdownSeconds.set(config.countdownSeconds)
        nextRoundWhenEveryoneDisconnects.set(config.nextRoundWhenEveryoneDisconnects)
        unsafeSkipWorldClose.set(config.unsafeSkipWorldClose)
        chatDefaultToTeamChat.set(config.chat.defaultToTeamChat)
        chatDefaultToSpectatorChat.set(config.chat.defaultToSpectatorChat)
        chatGlobalCommandAliases.set(config.chat.globalCommandAliases)
        chatTeamCommandAliases.set(config.chat.teamCommandAliases)
        databaseUrl.set(config.databaseUrl.orEmpty())
        databaseUser.set(config.databaseUser.orEmpty())
        databasePass.set(config.databasePass.orEmpty())
        syncStats.set(config.syncStats)
        statsHostId.set(config.statsHostId.toString())
        serverIsLobbyMode.set(config.server.isLobbyMode)
        serverPreloadViewDistance.set(config.server.preloadViewDistance)
        serverFilesToReset.set(config.server.filesToReset)
        defaultSeenTutorial.set(config.server.defaultPlayerSettings.seenTutorial)
        defaultHideLobbyPrompt.set(config.server.defaultPlayerSettings.hideLobbyPrompt)
        defaultBossbar.set(config.server.defaultPlayerSettings.bossbar)
        defaultScoreboard.set(config.server.defaultPlayerSettings.scoreboard)
        defaultScoreboardAutoHide.set(config.server.defaultPlayerSettings.scoreboardAutoHide)
        defaultLeadingMessages.set(config.server.defaultPlayerSettings.leadingMessages)
        defaultScoreMessages.set(config.server.defaultPlayerSettings.scoreMessages)
        defaultItemMessages.set(config.server.defaultPlayerSettings.itemMessages)
        defaultNightVision.set(config.server.defaultPlayerSettings.nightVision)
    }
}

