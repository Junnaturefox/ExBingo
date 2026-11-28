package me.jfenn.bingo.common.game

import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.common.commands.canConfigureGame
import me.jfenn.bingo.common.commands.formatWarning
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext

class GameCommands(
    commandManager: ICommandManager,
    private val text: TextProvider,
) : BingoComponent() {

    private fun IExecutionContext.start(ignoreWarnings: Boolean) {
        val gameService = scope.get<GameService>()

        scope.get<IExecutors.IServerTaskExecutor>().execute {
            // Start the game!
            val warnings = mutableListOf<IText>()
            val started = gameService.start(
                warnings = warnings,
                ignoreWarnings = ignoreWarnings,
                allowSpectators = ignoreWarnings,
            )

            // only report success when the game actually started - this used to always print
            // "Starting the game..." even when start() refused (e.g. players without teams)
            if (started) {
                sendFeedback(text.string(StringKey.CommandStartSuccess))
            }

            warnings.forEachIndexed { i, warning ->
                sendMessage(text.formatWarning(warning, i == 0))
            }
        }
    }

    init {
        commandManager.register("bingo") {
            literal("start") {
                requires {
                    hasState(GameState.PREGAME) && canConfigureGame()
                }

                executes { start(ignoreWarnings = false) }

                literal("ignore_warnings") {
                    executes { start(ignoreWarnings = true) }
                }
            }

            literal("end") {
                requires {
                    hasState(GameState.PLAYING) && canConfigureGame()
                }

                executes {
                    val gameService = scope.get<GameService>()
                    val state = scope.get<BingoState>()
                    state.isForfeit = true

                    val player = player
                    val playerName = when {
                        player != null -> player.playerName
                        isConsole -> "RCON"
                        else -> "[unknown]"
                    }

                    gameService.end(GameEndReason.Command(playerName))
                    sendFeedback(text.string(StringKey.CommandEndSuccess))
                }
            }

            literal("resume") {
                requires {
                    val resumeService = scope.get<GameResumeService>()
                    resumeService.isResumeAvailable(player)
                }
                executes {
                    val resumeService = scope.get<GameResumeService>()
                    resumeService.resume()
                    sendFeedback(text.string(StringKey.CommandResumeSuccess))
                }
            }

            literal("nextround") {
                requires {
                    // only meaningful once a round has finished
                    hasState(GameState.POSTGAME) && canConfigureGame()
                }
                executes {
                    val resetService = scope.get<me.jfenn.bingo.common.autorestart.ResetService>()
                    val gameService = scope.get<GameService>()
                    val serverTaskExecutor = scope.get<me.jfenn.bingo.platform.IExecutors.IServerTaskExecutor>()
                    val playerManager = scope.get<me.jfenn.bingo.platform.IPlayerManager>()
                    val bingoState = scope.get<BingoState>()

                    // Starting a round with nobody online would leave it in STARTING_NEXT_ROUND
                    // (GameService.start refuses), so refuse here instead.
                    if (playerManager.getPlayers().isEmpty()) {
                        sendMessage(text.string(StringKey.CommandStartNoPlayers))
                        return@executes
                    }

                    // The reset clears the teams, so remember how many there were and re-shuffle
                    // afterwards - otherwise every player would be treated as a spectator.
                    val teamCount = bingoState.getRegisteredTeams().size.coerceAtLeast(1)

                    serverTaskExecutor.execute {
                        // Reset straight into STARTING_NEXT_ROUND (rather than PREGAME) so the
                        // round can be started again without an intermediate lobby phase.
                        resetService.resetGame(GameState.STARTING_NEXT_ROUND)

                        if (bingoState.state != GameState.STARTING_NEXT_ROUND) {
                            // the reset refused (e.g. it is not lobby mode) - do not claim success
                            sendMessage(text.string(StringKey.CommandNextRoundFailed))
                            return@execute
                        }

                        val teamService = scope.get<me.jfenn.bingo.common.team.TeamService>()
                        if (bingoState.getRegisteredTeams().isEmpty()) {
                            teamService.shuffleTeams(teamCount)
                        }

                        val warnings = mutableListOf<IText>()
                        val started = gameService.start(
                            warnings = warnings,
                            ignoreWarnings = true,
                            allowSpectators = true,
                        )
                        if (started) {
                            sendFeedback(text.string(StringKey.CommandNextRoundStarting))
                        }
                        for ((index, warning) in warnings.withIndex()) {
                            sendMessage(text.formatWarning(warning, index == 0))
                        }
                    }
                }
            }
            literal("world") {
                literal("seed") {
                    requires { canConfigureGame() }

                    executes {
                        val state = scope.get<BingoState>()
                        val seed = state.worldSeed
                        sendFeedback(
                            if (seed != null) text.string(StringKey.CommandWorldSeedCurrent, seed.toString())
                            else text.string(StringKey.CommandWorldSeedRandom)
                        )
                    }

                    long("seed") { seedArg ->
                        executes {
                            val state = scope.get<BingoState>()
                            val seed = getArgument(seedArg)
                            state.worldSeed = seed
                            sendFeedback(text.string(StringKey.CommandWorldSeedSet, seed.toString()))
                        }
                    }

                    literal("random") {
                        executes {
                            val state = scope.get<BingoState>()
                            state.worldSeed = null
                            sendFeedback(text.string(StringKey.CommandWorldSeedRandomSet))
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val START_COMMAND = "/bingo start"
        const val IGNORE_WARNINGS_COMMAND = "/bingo start ignore_warnings"
        const val END_COMMAND = "/bingo end"
        const val RESUME_COMMAND = "/bingo resume"
        const val NEXT_ROUND_COMMAND = "/bingo nextround"
    }
}
