package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.menu.lifecycle.MenuEntityStats

internal interface RuntimeLobbyController {
    fun prepareLobbyFiles()
    fun suspendPregameSpawn()
    fun spawnLobby()
    fun menuEntityStats(): MenuEntityStats
}
