package me.jfenn.bingo.common.team

/**
 * Helpers for handing players out to teams during `/bingo shuffleteams`.
 *
 * Kept separate from [TeamService] so the distribution can be unit tested without a running
 * server: players are assigned round-robin, which keeps the team sizes within one player of each
 * other (e.g. 7 players over 3 teams -> 3, 2, 2) and never produces a team with everyone else
 * lumped onto it, which the previous `ceil` + `chunked` approach did (7 over 3 -> 3, 3, 1).
 */
internal object TeamShuffler {

    /** Team sizes for [playerCount] players spread over [teamCount] teams, in assignment order. */
    fun teamSizes(playerCount: Int, teamCount: Int): List<Int> {
        if (teamCount <= 0) return emptyList()

        val base = playerCount / teamCount
        val remainder = playerCount % teamCount
        return List(teamCount) { index -> base + if (index < remainder) 1 else 0 }
    }

    /** Team index that the player at [playerIndex] is assigned to. */
    fun teamIndex(playerIndex: Int, teamCount: Int): Int {
        require(teamCount > 0) { "teamCount must be positive" }
        return playerIndex % teamCount
    }
}
