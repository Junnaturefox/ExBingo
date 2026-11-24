package me.jfenn.bingo.common.lobby

import me.jfenn.bingo.common.config.ServerConfig

/**
 * Lobby mode is a property of a single save, not of the whole server.
 *
 * The lobby dimension is provided by a datapack inside `<world>/datapacks/`, so it is per save; the
 * request that installs (or removes) it, however, is stored in the global server config. That
 * request is therefore bound to the save (`level-name`) it was made for, so enabling lobby mode in
 * one world cannot silently enable it in every other world.
 */
internal object LobbyModeTarget {

    /**
     * True when the stored request targets [levelName].
     *
     * A request without a save name is a legacy config (written before the binding existed); it is
     * treated as applying to whichever save is loaded, and gets bound to that save on startup.
     */
    fun appliesTo(server: ServerConfig, levelName: String): Boolean {
        val target = server.isLobbyModeWorld
        return target == null || target == levelName
    }

    /** Lobby mode is enabled (or requested) for [levelName]. */
    fun isLobbyModeFor(server: ServerConfig, levelName: String): Boolean =
        server.isLobbyMode && appliesTo(server, levelName)
}
