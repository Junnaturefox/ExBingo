package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    var isLobbyMode: Boolean = false,
    /**
     * The save (`level-name`) the [isLobbyMode] request applies to.
     *
     * Lobby mode is provided by a datapack inside the save folder, so it is a property of a single
     * save — but this request has to live in the global server config. Binding it to a save name
     * keeps enabling lobby mode in one world from silently enabling it in every other world.
     * `null` means "not bound yet" (configs written before this field existed).
     */
    var isLobbyModeWorld: String? = null,
    val preloadViewDistance: Int = 4,
    val performanceCleanup: PerformanceCleanupConfig = PerformanceCleanupConfig(),
    val filesToReset: List<String> = listOf(
        "advancements",
        "data/bingo.dat",
        "data/idcounts.dat",
        "data/map_*.dat",
        "data/raids.dat",
        "data/random_sequences.dat",
        "DIM*",
        "dimensions",
        "entities",
        "playerdata",
        "poi",
        "region",
        "stats",
        "level.dat",
        "level.dat_old",
    ),
    val defaultPlayerSettings: PlayerSettings = PlayerSettings(),
)

@Serializable
data class PerformanceCleanupConfig(
    val enabled: Boolean = true,
    val intervalTicks: Int = 400,
    val minTotalEntities: Int = 1600,
    val keepChunkRadius: Int = 12,
    val maxEntitiesPerPass: Int = 160,
    val cleanupItems: Boolean = true,
    val cleanupProjectiles: Boolean = true,
    val cleanupMobs: Boolean = true,
    val cleanupMultipartEntities: Boolean = true,
    val protectedEntityNamespaces: Set<String> = setOf(
        "exbingo",
        "minecraft:player",
    ),
)
