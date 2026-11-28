package me.jfenn.bingo.common.ready

import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.autorestart.ResetService
import me.jfenn.bingo.common.commands.formatWarning
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatHHMMSS
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.permissions.IPermissionsApi
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.text.IText
import net.minecraft.network.chat.Component
import org.slf4j.Logger
import java.time.Duration

internal class ReadyController(
    private val log: Logger,
    private val readyState: ReadyTimerState,
    private val state: BingoState,
    private val config: BingoConfig,
    private val text: TextProvider,
    private val gameService: GameService,
    private val playerManager: IPlayerManager,
    private val resetService: ResetService,
    private val packets: ServerPacketEvents,
    private val permissions: IPermissionsApi,
    private val readyService: ReadyService,
    private val teamService: me.jfenn.bingo.common.team.TeamService,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
    events: ScopedEvents,
) {

    private fun update(
        playersToUpdate: List<IPlayerHandle> = playerManager.getPlayers(),
    ) {
        if (!state.isLobbyMode) return
        // Only send update packets during these gamestates
        // (note that SpawnPreloadingController also sends ReadyUpdatePacket, so this must avoid conflicts)
        if (state.state !in arrayOf(GameState.PREGAME, GameState.POSTGAME))
            return

        val isVotingEnabled = when (state.state) {
            GameState.PREGAME -> config.startWhenReadySeconds != null
            GameState.POSTGAME -> config.nextRoundWhenReadySeconds != null
            else -> false
        }

        readyState.updatePlayers(playerManager.getPlayers().filter { readyService.canUseReady(it) })
        val remainingTime = readyState.remainingTime()
        val isComplete = remainingTime?.let { it <= Duration.ZERO } ?: false

        readyService.tick()

        if (isComplete && state.state == GameState.PREGAME) {
            log.info("[ReadyController] Reached the end of the PREGAME timer - starting the game!")
            readyService.reset()
            val warnings = mutableListOf<IText>()
            // the ready countdown is the players' confirmation to start, so warnings must not stop
            // it - without ignoreWarnings, start() returns false and the timer restarts forever
            val started = gameService.start(
                warnings = warnings,
                ignoreWarnings = true,
                allowSpectators = true,
            )
            warnings.forEachIndexed { i, warning ->
                playerManager.getPlayers()
                    .forEach { it.sendMessage(text.formatWarning(warning, i == 0)) }
            }
            if (!started) {
                // avoid an endless retry loop: the timer would otherwise restart on the next tick
                log.warn("[ReadyController] The ready auto-start was refused; cancelling the timer.")
                readyState.isCancelled = true
            }
        }

        if (isComplete && state.state == GameState.POSTGAME) {
            val vote = readyService.winningVote() ?: VoteType.PREGAME
            // the reset clears the teams, so remember how many there were for the next round
            val teamCount = state.getRegisteredTeams().size.coerceAtLeast(1)

            // clear the timer synchronously: update() runs from several entry points, and without
            // this the whole reset/start block below could be dispatched more than once
            readyService.reset()

            log.info("[ReadyController] Reached the end of the POSTGAME timer - running {} (vote).", vote)
            serverTaskExecutor.execute {
                when (vote) {
                    VoteType.NEXT_ROUND -> {
                        resetService.resetGame(GameState.STARTING_NEXT_ROUND)
                        if (state.state != GameState.STARTING_NEXT_ROUND) {
                            log.warn("[ReadyController] The reset was refused; leaving the state as-is.")
                            return@execute
                        }

                        if (state.getRegisteredTeams().isEmpty() && playerManager.getPlayers().isNotEmpty()) {
                            teamService.shuffleTeams(teamCount)
                        }

                        val warnings = mutableListOf<IText>()
                        val started = gameService.start(
                            warnings = warnings,
                            ignoreWarnings = true,
                            allowSpectators = true,
                        )
                        if (started) {
                            for ((index, warning) in warnings.withIndex()) {
                                playerManager.getPlayers().forEach { player ->
                                    player.sendMessage(text.formatWarning(warning, index == 0))
                                }
                            }
                        } else {
                            // GameService.start refuses when nobody is online / options are invalid;
                            // fall back to the lobby so the server is not left in STARTING_NEXT_ROUND
                            log.warn("[ReadyController] Failed to start the next round; falling back to the lobby.")
                            resetService.resetGame()
                        }
                    }
                    VoteType.PREGAME -> resetService.resetGame()
                }
            }
        }

        val updatePacket = ReadyUpdatePacket(
            isRunning = readyState.isRunning && !isComplete,
            isReady = false,
            state = state.state,
            remainingDuration = readyState.remainingTime() ?: Duration.ZERO,
            totalDuration = readyState.totalTime() ?: Duration.ZERO,
            readyPlayers = playerManager.getPlayers().count { readyState.isReady(it.uuid) },
            totalPlayers = playerManager.getPlayers().size,
            title = null,
            subtitle = null,
        )

        for (player in playersToUpdate) {
            val isPlayerReady = readyState.isReady(player.uuid)
            val title = when {
                isPlayerReady -> text.string(StringKey.LobbyStartingPlayersReady, updatePacket.readyPlayers, updatePacket.totalPlayers)
                readyService.canUseReady(player) -> text.string(
                    StringKey.LobbyStartingReadyUp,
                    Component.keybind("key.sneak"), Component.keybind(KEYBIND_OPEN_CARD)
                )
                else -> text.empty()
            }
            val subtitle = when {
                state.state == GameState.PREGAME -> text.string(StringKey.LobbyStartingTimeRemaining, updatePacket.remainingDuration.formatHHMMSS())
                else -> text.string(StringKey.LobbyNextRoundTimeRemaining, updatePacket.remainingDuration.formatHHMMSS())
            }

            val packet = updatePacket.copy(
                isReady = isPlayerReady,
                title = title,
                subtitle = subtitle,
                canSendReady = isVotingEnabled && permissions.hasPermission(player, Permission.COMMAND_READY),
                votes = if (state.state == GameState.POSTGAME && readyState.isRunning) readyService.voteCounts() else emptyMap(),
                myVote = if (state.state == GameState.POSTGAME) readyService.myVote(player) else null,
            )

            when {
                packets.readyUpdateV4.send(player, packet) -> {}
                packets.readyUpdateV3.send(player, packet) -> {}
                packets.readyUpdateV2.send(player, packet) -> {}
                packets.readyUpdateV1.send(player, packet) -> {}
                packet.isRunning -> {
                    val message = when {
                        !isPlayerReady -> text.string(
                            when (state.state) {
                                GameState.POSTGAME -> StringKey.LobbyLeavingInRunCommand
                                else -> StringKey.LobbyStartingInRunCommand
                            },
                            packet.remainingDuration.formatHHMMSS()
                        )
                        else -> text.string(
                            when (state.state) {
                                GameState.POSTGAME -> StringKey.LobbyLeavingInReady
                                else -> StringKey.LobbyStartingInReady
                            },
                            packet.remainingDuration.formatHHMMSS(),
                            packet.readyPlayers,
                            packet.totalPlayers
                        )
                    }
                    player.sendHotbarMessage(message)
                }
            }
        }
    }

    init {
        events.onStateChange {
            readyService.reset()

            // Run an update check immediately
            update()

            // If the timer is not running, send an extra packet to dismiss the ready GUI.
            // Skip this during the loading phases (STARTING/PRELOADING/LOADING): there
            // SpawnPreloadingController owns the same ReadyUpdatePacket to drive the
            // terrain-loading progress bar, and a dismiss here blanks it out on every
            // state transition — which is exactly the bar flashing 0% then vanishing
            // until the load screen. Those states are left to SpawnPreloadingController;
            // COUNTDOWN/PLAYING/PREGAME/POSTGAME still dismiss to clear the bar (e.g. so
            // the finished loading bar is closed when the game actually begins).
            val isLoadingPhase = state.state in arrayOf(
                GameState.STARTING, GameState.PRELOADING, GameState.LOADING,
            )
            if (!readyState.isRunning && !isLoadingPhase) {
                val packet = ReadyUpdatePacket(
                    isRunning = false,
                    isReady = false,
                    state = state.state,
                    remainingDuration = Duration.ZERO,
                    totalDuration = Duration.ZERO,
                    readyPlayers = 0,
                    totalPlayers = 0,
                    title = null,
                    subtitle = null,
                )
                for (player in playerManager.getPlayers()) {
                    when {
                        packets.readyUpdateV3.send(player, packet) -> {}
                        packets.readyUpdateV2.send(player, packet) -> {}
                        packets.readyUpdateV1.send(player, packet) -> {}
                    }
                }
            }
        }

        events.onChangeOptions {
            if (readyState.isRunning) {
                log.info("[ReadyController] Reset the timer, as the game options have changed")
            }
            readyService.reset()
        }

        events.onChangeTeam {
            // If the player leaves their team, un-ready them
            if (it.team == null) {
                readyState.setReady(it.player.uuid, false)
                playerManager.updatePlayerListName(it.player)
            }
            // If the player changes team while ready, update the tab name
            if (readyState.isReady(it.player.uuid)) {
                playerManager.updatePlayerListName(it.player)
            }
        }

        events.onPacket(packets.readyVoteV1) {
            val result = readyService.vote(it.player, it.packet.option)
            it.player.sendMessage(result.message)
            update()
        }

        events.onPacket(packets.readySetV1) {
            val isReady = it.packet.isReady
            val result = readyService.setReady(it.player, isReady)
            // Only send the chat message if the player is in multiplayer (or if it failed)
            if (!state.isSingleplayer() || result !is ReadyResult.ChangedTo) {
                it.player.sendMessage(result.message)
            }
        }

        events.onPlayerChannelRegister {
            update(listOf(it.player))
        }

        events.onUpdateTick {
            update()
        }
    }
}
