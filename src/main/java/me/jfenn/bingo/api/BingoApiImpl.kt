package me.jfenn.bingo.api

import me.jfenn.bingo.api.config.IBingoConfig
import me.jfenn.bingo.api.data.*
import me.jfenn.bingo.api.event.GameEndedEvent
import me.jfenn.bingo.api.event.GameStartedEvent
import me.jfenn.bingo.api.event.TeamChangedEvent
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.options.BingoGoal
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.stats.StatsService
import net.minecraft.server.MinecraftServer
import java.util.*

internal class BingoApiImpl(
    override val server: MinecraftServer,
    private val state: BingoState,
    private val statsService: StatsService,
    events: ScopedEvents,
) : IBingoApi, IBingoTeams, IBingoConfig {
    override val game: BingoGame
        get() = BingoGame(
            id = state.gameId,
            status = when (state.state) {
                GameState.UNINITIALIZED,
                GameState.PREGAME -> BingoGameStatus.PREGAME
                GameState.STARTING,
                GameState.PRELOADING,
                GameState.LOADING,
                GameState.COUNTDOWN -> BingoGameStatus.STARTING
                GameState.PLAYING -> BingoGameStatus.PLAYING
                GameState.POSTGAME,
                GameState.STARTING_NEXT_ROUND -> BingoGameStatus.POSTGAME
            },
            time = state.ingameDuration(),
            timeRemaining = state.remainingDuration()
        )

    override val card: IBingoCard?
        get() {
            val activeCard = state.cards.firstOrNull() ?: return null
            val cardOptions = activeCard.options
            return BingoCardImpl(
                id = activeCard.id,
                seed = activeCard.seed,
                entries = activeCard.entries.mapIndexed { index, entry ->
                    BingoCardEntryImpl(
                        objectiveId = entry.objectiveId,
                        x = index % 5,
                        y = index / 5,
                        tier = entry.tier?.name,
                        source = entry.source,
                        tileName = entry.tileName,
                    )
                },
                options = BingoCardOptionsImpl(
                    goalType = when (cardOptions.goal) {
                        is BingoGoal.Items -> "items"
                        is BingoGoal.Lines -> "lines"
                    },
                    goalCount = when (val goal = cardOptions.goal) {
                        is BingoGoal.Items -> goal.items
                        is BingoGoal.Lines -> goal.lines
                    },
                    distribution = cardOptions.itemDistribution.toList(),
                    isLockoutMode = cardOptions.isLockoutMode,
                    isInventoryMode = cardOptions.isInventoryMode,
                    isHiddenItemsMode = cardOptions.isHiddenItemsMode,
                    isConsumeItemsMode = cardOptions.isConsumeItemsMode,
                ),
            )
        }
    override val teams: IBingoTeams get() = this

    override fun getPlayerStats(id: UUID): IBingoPlayerStats {
        val stats = statsService.getPlayerSummary(id)
        return BingoPlayerStatsImpl(stats)
    }

    override val config: IBingoConfig get() = this

    override fun iterator(): Iterator<IBingoTeam> {
        return state.getRegisteredTeams()
            .map { BingoTeamImpl(it) }
            .iterator()
    }

    override val isLobbyMode: Boolean
        get() = state.isLobbyMode

    init {
        events.onEnter(GameState.PREGAME) {
            BingoEvents.GAME_RESET.invoke(null)
        }

        events.onEnter(GameState.STARTING, true) {
            BingoEvents.GAME_STARTING.invoke(null)
        }

        events.onEnter(GameState.PLAYING, true) {
            BingoEvents.GAME_STARTED.invoke(GameStartedEvent(game.id))
        }

        events.onEnter(GameState.POSTGAME, true) {
            BingoEvents.GAME_ENDED.invoke(GameEndedEvent(game.id))
        }

        events.onChangeTeam { event ->
            BingoEvents.TEAM_CHANGED.invoke(TeamChangedEvent(event.player.uuid))
        }
    }
}
