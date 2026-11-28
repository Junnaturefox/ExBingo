package me.jfenn.bingo.common.state

/**
 * Recovery rules that run when a save is loaded.
 *
 * A save can legitimately contain a state the game cannot continue from: the server may have been
 * killed between `reset()` and the state change, leaving a lobby save in POSTGAME with no cards,
 * teams or players. Every rule here is named, documented and unit-tested instead of being an inline
 * condition buried in the load path.
 */
internal object StateRecovery {

    /**
     * A lobby save that stopped in the middle of a reset: POSTGAME (or the transient
     * STARTING_NEXT_ROUND that exists only between the reset and the next start) with no cards,
     * teams or players.
     *
     * No command accepts those states, so the save is rebuilt as a fresh PREGAME lobby. A save with
     * any cards, teams or players is kept as-is - a live game must never be wiped by this rule.
     */
    fun isInterruptedLobbyReset(
        isLobbyMode: Boolean,
        state: GameState,
        cardCount: Int,
        teamCount: Int,
        playerCount: Int,
    ): Boolean = isLobbyMode &&
        (state == GameState.POSTGAME || state == GameState.STARTING_NEXT_ROUND) &&
        cardCount == 0 &&
        teamCount == 0 &&
        playerCount == 0
}

internal fun BingoState.isInterruptedLobbyReset(): Boolean = StateRecovery.isInterruptedLobbyReset(
    isLobbyMode = isLobbyMode,
    state = this.state,
    cardCount = cards.size,
    teamCount = teams.size,
    playerCount = players.size,
)
