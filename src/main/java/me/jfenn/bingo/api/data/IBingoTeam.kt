package me.jfenn.bingo.api.data

import java.util.*

interface IBingoTeam {
    val id: String
    val players: List<UUID>
    val score: BingoTeamScore
}
