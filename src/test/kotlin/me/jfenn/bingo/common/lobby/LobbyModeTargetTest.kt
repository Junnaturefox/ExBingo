package me.jfenn.bingo.common.lobby

import me.jfenn.bingo.common.config.ServerConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LobbyModeTargetTest {

    @Test
    fun `a request bound to another save does not apply`() {
        val config = ServerConfig(isLobbyMode = true, isLobbyModeWorld = "world_a")

        assertTrue(LobbyModeTarget.appliesTo(config, "world_a"))
        assertTrue(LobbyModeTarget.isLobbyModeFor(config, "world_a"))

        // the world that was never asked for must stay untouched
        assertFalse(LobbyModeTarget.appliesTo(config, "world_b"))
        assertFalse(LobbyModeTarget.isLobbyModeFor(config, "world_b"))
    }

    @Test
    fun `a legacy request without a save name applies so it can be bound`() {
        val config = ServerConfig(isLobbyMode = true, isLobbyModeWorld = null)

        assertTrue(LobbyModeTarget.appliesTo(config, "any_world"))
        assertTrue(LobbyModeTarget.isLobbyModeFor(config, "any_world"))
    }

    @Test
    fun `a disabled request applies to nothing`() {
        val config = ServerConfig(isLobbyMode = false, isLobbyModeWorld = "world_a")

        assertTrue(LobbyModeTarget.appliesTo(config, "world_a"), "the pending removal still targets world_a")
        assertFalse(LobbyModeTarget.isLobbyModeFor(config, "world_a"))
        assertTrue(LobbyModeTarget.isLobbyModeFor(
            ServerConfig(isLobbyMode = false, isLobbyModeWorld = null),
            "world_a",
        ).not())
    }
}
