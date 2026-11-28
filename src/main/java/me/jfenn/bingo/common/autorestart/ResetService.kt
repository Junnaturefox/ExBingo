package me.jfenn.bingo.common.autorestart

import me.jfenn.bingo.common.bossbar.ResetBossBarService
import me.jfenn.bingo.common.menu.RuntimeLobbyController
import me.jfenn.bingo.common.scoreboard.ResetScoreboardService
import me.jfenn.bingo.common.spawn.LobbyPlayerRestorer
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.state.ResetPersistentStates
import me.jfenn.bingo.common.team.ResetTeamService
import me.jfenn.bingo.common.utils.measureTime
import me.jfenn.bingo.platform.IPersistentStateManager
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IServerWorldFactory
import me.jfenn.bingo.platform.ITickManager
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.GameResetEvent
import org.slf4j.Logger
import kotlin.random.Random

internal class ResetService(
    private val eventBus: IEventBus,
    private val state: BingoState,
    private val playerManager: IPlayerManager,
    private val playerController: LobbyPlayerRestorer,
    private val teamService: ResetTeamService,
    private val scoreboardService: ResetScoreboardService,
    private val bossBarService: ResetBossBarService,
    private val serverWorldFactory: IServerWorldFactory,
    private val menuController: RuntimeLobbyController,
    private val persistentStateManager: IPersistentStateManager,
    private val persistentStates: ResetPersistentStates,
    private val tickManager: ITickManager,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
    private val offlinePlayerCache: me.jfenn.bingo.common.team.OfflinePlayerCache,
    private val log: Logger,
) {
    @Volatile
    private var isResetting = false

    fun resetGame(newState: GameState = GameState.PREGAME) {
        if (!state.isLobbyMode) {
            log.error("[Reset] resetGame() called outside of lobby mode - ignoring.")
            return
        }

        // Two triggers can land in the same tick (two ops running /bingo reset, or two players
        // disconnecting together); running the world recreation twice would invalidate the world
        // objects that the first reset already scheduled work against.
        if (isResetting) {
            log.warn("[Reset] resetGame() is already in progress - ignoring duplicate request.")
            return
        }

        isResetting = true
        try {
            tickManager.setFrozen(false)
            resetGameWorlds(newState)
        } catch (e: Throwable) {
            log.error("[Reset] resetGameWorlds() failed", e)
            throw e
        } finally {
            isResetting = false
            tickManager.setFrozen(false)
        }
    }

    private fun resetGameWorlds(newState: GameState) = log.measureTime("Resetting game...") {
        // Clear scoreboards (otherwise this causes an error when recreated)
        // capture the seed before state.reset() clears the per-round fields
        val nextWorldSeed = state.worldSeed ?: Random.Default.nextLong()

        log.info("[Reset] Clearing game data")
        teamService.clearTeams()
        scoreboardService.clearScoreboards()
        bossBarService.clearBossBars()
        state.reset()

        // drop cached offline player handles *before* the levels are recreated - they point at the
        // old (about to be closed) ServerLevels
        offlinePlayerCache.clear()

        if (dynamicWorldRecreateOnReset()) {
            log.info("[Reset] Recreating worlds (fresh world per round)")
            tickManager.setFrozen(false)
            serverWorldFactory.recreateWorlds(nextWorldSeed) {
                menuController.prepareLobbyFiles()
            }
            tickManager.setFrozen(false)
        } else {
            log.warn("[Reset] dynamicWorldRecreateOnReset=false; skipping live world recreation (previous round's terrain will persist)")
            tickManager.setFrozen(false)
        }

        log.info("[Reset] Teleporting players back to lobby")
        menuController.suspendPregameSpawn()
        state.changeState(eventBus, newState) // invokes createInitialCards in ScoredItemCheck

        for (player in playerManager.getPlayers()) {
            playerController.restoreLobbyPlayerAfterReset(player)
        }

        log.info("[Reset] Transferring game state")
        // This is important! Otherwise the bingo persistent state does not get copied into the new world data
        // (ScopeManager does this on initial setup, but it gets lost when the world is replaced)
        persistentStateManager.put(
            type = persistentStates.bingo,
            value = state,
        )

        serverTaskExecutor.executeNextTick {
            tickManager.setFrozen(false)
            for (player in playerManager.getPlayers()) {
                player.resyncClientState(syncPosition = true)
            }

            serverTaskExecutor.executeNextTick {
                tickManager.setFrozen(false)
                menuController.spawnLobby()
                eventBus.emit(GameResetEvent, GameResetEvent())

                serverTaskExecutor.executeNextTick {
                    tickManager.setFrozen(false)
                    for (player in playerManager.getPlayers()) {
                        player.resyncClientState(syncPosition = true)
                    }
                    requestPostResetGc()
                }
            }
        }
    }

    private fun requestPostResetGc() {
        val runtime = Runtime.getRuntime()
        val beforeUsed = runtime.totalMemory() - runtime.freeMemory()
        log.info(
            "[Reset] Requesting post-reset GC (heapUsedBefore={} MiB, heapMax={} MiB)",
            beforeUsed / BYTES_PER_MIB,
            runtime.maxMemory() / BYTES_PER_MIB,
        )
        System.gc()
        serverTaskExecutor.executeNextTick {
            val afterUsed = runtime.totalMemory() - runtime.freeMemory()
            log.info(
                "[Reset] Post-reset GC check (heapUsedAfter={} MiB, heapMax={} MiB)",
                afterUsed / BYTES_PER_MIB,
                runtime.maxMemory() / BYTES_PER_MIB,
            )
        }
    }

    private fun dynamicWorldRecreateOnReset(): Boolean {
        // Default to recreating worlds on reset (so each round starts on a fresh world with no
        // leftover terrain/builds from the previous game). Can be explicitly disabled by setting
        // -Dexbingo.dynamicWorldRecreateOnReset=false as an escape hatch if live recreation misbehaves.
        val raw = System.getProperty(DYNAMIC_WORLD_RECREATE_ON_RESET_PROPERTY)
            ?: return DYNAMIC_WORLD_RECREATE_ON_RESET_DEFAULT
        return raw.toBoolean()
    }

    private companion object {
        const val DYNAMIC_WORLD_RECREATE_ON_RESET_PROPERTY = "exbingo.dynamicWorldRecreateOnReset"
        const val DYNAMIC_WORLD_RECREATE_ON_RESET_DEFAULT = true
        const val BYTES_PER_MIB = 1024 * 1024
    }

}
