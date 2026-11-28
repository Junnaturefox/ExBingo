package me.jfenn.bingo.common.menu.lifecycle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LobbyEntityTagsTest {

    private val own = "bingo-11111111-1111-1111-1111-111111111111"
    private val other = "bingo-22222222-2222-2222-2222-222222222222"

    @Test
    fun `only bingo tags are considered lobby tags`() {
        assertEquals(listOf(own), listOf(own, "exbingo-menuitem", "some-other").lobbyTags())
        assertEquals(emptyList<String>(), listOf("exbingo-menuitem", "bingo").lobbyTags())
    }

    @Test
    fun `an entity tagged by this instance is not stale`() {
        assertTrue(listOf(own, "unrelated").isLobbyEntityOf(own))
        assertFalse(listOf(own, "unrelated").isStaleLobbyEntity(own))
    }

    @Test
    fun `an entity tagged by another instance is stale`() {
        assertTrue(listOf(other).isStaleLobbyEntity(own))
        // a stale entity can carry several bingo tags, e.g. a team picker tag plus a marker tag
        assertTrue(listOf(other, "bingo-33333333-3333-3333-3333-333333333333").isStaleLobbyEntity(own))
        assertFalse(listOf(other).isLobbyEntityOf(own))
    }

    @Test
    fun `entities without bingo tags are never swept`() {
        // ordinary world entities must survive a menu rebuild
        assertFalse(emptyList<String>().isStaleLobbyEntity(own))
        assertFalse(listOf("unrelated", "exbingo-menuitem").isStaleLobbyEntity(own))
    }

    @Test
    fun `instance tags are unique and prefixed`() {
        val a = newLobbyInstanceTag()
        val b = newLobbyInstanceTag()
        assertTrue(a.startsWith(LOBBY_TAG_PREFIX))
        assertTrue(b.startsWith(LOBBY_TAG_PREFIX))
        assertTrue(a != b)
    }
}
