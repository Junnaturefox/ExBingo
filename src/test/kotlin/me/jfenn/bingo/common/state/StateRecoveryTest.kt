package me.jfenn.bingo.common.state

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateRecoveryTest {

    @Test
    fun `an interrupted lobby reset is recovered`() {
        assertTrue(
            StateRecovery.isInterruptedLobbyReset(
                isLobbyMode = true,
                state = GameState.POSTGAME,
                cardCount = 0,
                teamCount = 0,
                playerCount = 0,
            )
        )
        assertTrue(
            StateRecovery.isInterruptedLobbyReset(
                isLobbyMode = true,
                state = GameState.STARTING_NEXT_ROUND,
                cardCount = 0,
                teamCount = 0,
                playerCount = 0,
            )
        )
    }

    @Test
    fun `a postgame save that still has data is kept`() {
        assertFalse(
            StateRecovery.isInterruptedLobbyReset(
                isLobbyMode = true,
                state = GameState.POSTGAME,
                cardCount = 4,
                teamCount = 0,
                playerCount = 0,
            )
        )
        assertFalse(
            StateRecovery.isInterruptedLobbyReset(
                isLobbyMode = true,
                state = GameState.POSTGAME,
                cardCount = 0,
                teamCount = 2,
                playerCount = 0,
            )
        )
        assertFalse(
            StateRecovery.isInterruptedLobbyReset(
                isLobbyMode = true,
                state = GameState.POSTGAME,
                cardCount = 0,
                teamCount = 0,
                playerCount = 1,
            )
        )
    }

    @Test
    fun `a live round is never treated as a broken save`() {
        for (state in listOf(
            GameState.PREGAME,
            GameState.STARTING,
            GameState.PRELOADING,
            GameState.LOADING,
            GameState.COUNTDOWN,
            GameState.PLAYING,
            GameState.UNINITIALIZED,
        )) {
            assertFalse(
                StateRecovery.isInterruptedLobbyReset(
                    isLobbyMode = true,
                    state = state,
                    cardCount = 0,
                    teamCount = 0,
                    playerCount = 0,
                ),
                "$state must not be rebuilt as a fresh lobby",
            )
        }
    }

    @Test
    fun `non-lobby saves are never recovered`() {
        assertFalse(
            StateRecovery.isInterruptedLobbyReset(
                isLobbyMode = false,
                state = GameState.POSTGAME,
                cardCount = 0,
                teamCount = 0,
                playerCount = 0,
            )
        )
    }
}
