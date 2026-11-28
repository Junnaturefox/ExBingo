package me.jfenn.bingo.common.team

import kotlin.test.Test
import kotlin.test.assertEquals

class TeamShufflerTest {

    @Test
    fun `players are spread evenly across teams`() {
        assertEquals(listOf(3, 2, 2), TeamShuffler.teamSizes(playerCount = 7, teamCount = 3))
        assertEquals(listOf(3, 3, 2, 2), TeamShuffler.teamSizes(playerCount = 10, teamCount = 4))
        assertEquals(listOf(2, 2), TeamShuffler.teamSizes(playerCount = 4, teamCount = 2))
        assertEquals(listOf(1, 1, 0), TeamShuffler.teamSizes(playerCount = 2, teamCount = 3))
        assertEquals(listOf(0, 0, 0, 0), TeamShuffler.teamSizes(playerCount = 0, teamCount = 4))
    }

    @Test
    fun `no team gets more than one extra player`() {
        for (players in 0..50) {
            for (teams in 1..12) {
                val sizes = TeamShuffler.teamSizes(players, teams)
                assertEquals(players, sizes.sum(), "players must all be assigned ($players/$teams)")
                assertEquals(teams, sizes.size)
                val spread = sizes.max() - sizes.min()
                check(spread <= 1) { "team sizes differ by $spread for $players players / $teams teams: $sizes" }
            }
        }
    }

    @Test
    fun `assignment order matches the team sizes`() {
        val assigned = (0 until 7).map { TeamShuffler.teamIndex(it, 3) }
        assertEquals(listOf(0, 1, 2, 0, 1, 2, 0), assigned)
        assertEquals(
            TeamShuffler.teamSizes(7, 3),
            List(3) { team -> assigned.count { it == team } },
        )
    }
}
