package me.jfenn.bingo.common.ready

import me.jfenn.bingo.generated.StringKey

/**
 * The action a player votes for while the post-game ready countdown is running.
 */
enum class VoteType(val label: StringKey) {
    /** Reset and start the next round straight away (skips the lobby phase). */
    NEXT_ROUND(StringKey.VoteNextRound),

    /** Reset back to the lobby without starting another round. */
    PREGAME(StringKey.VoteReturnToLobby),
}
