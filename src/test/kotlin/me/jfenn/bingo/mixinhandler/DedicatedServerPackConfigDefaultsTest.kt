package me.jfenn.bingo.mixinhandler

import kotlin.test.Test
import kotlin.test.assertEquals

class DedicatedServerPackConfigDefaultsTest {

    private val lobbyPack = "file/exbingo.zip"

    @Test
    fun `an enabled lobby pack is left untouched`() {
        val (enabled, disabled) = DedicatedServerPackConfigDefaults.enablePack(
            enabled = listOf("vanilla", lobbyPack),
            disabled = listOf("file/other.zip"),
            packId = lobbyPack,
        )

        assertEquals(listOf("vanilla", lobbyPack), enabled)
        assertEquals(listOf("file/other.zip"), disabled)
    }

    @Test
    fun `a missing lobby pack is appended to the enabled list`() {
        val (enabled, disabled) = DedicatedServerPackConfigDefaults.enablePack(
            enabled = listOf("vanilla", "bundle"),
            disabled = listOf("file/other.zip"),
            packId = lobbyPack,
        )

        assertEquals(listOf("vanilla", "bundle", lobbyPack), enabled)
        assertEquals(listOf("file/other.zip"), disabled)
    }

    @Test
    fun `a lobby pack recorded as disabled is re-enabled`() {
        val (enabled, disabled) = DedicatedServerPackConfigDefaults.enablePack(
            enabled = listOf("vanilla"),
            disabled = listOf(lobbyPack, "file/other.zip"),
            packId = lobbyPack,
        )

        assertEquals(listOf("vanilla", lobbyPack), enabled)
        assertEquals(listOf("file/other.zip"), disabled)
    }
}
