package me.jfenn.bingo.client.common.teamchest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeamChestButtonLayoutTest {

    /** Width of the survival inventory GUI. */
    private val inventoryWidth = 176

    @Test
    fun `button sits immediately right of the recipe book button`() {
        val recipeBookX = 0 + TeamChestButtonLayout.RECIPE_BOOK_OFFSET_X
        val x = TeamChestButtonLayout.x(0)

        assertEquals(recipeBookX + TeamChestButtonLayout.RECIPE_BOOK_WIDTH + TeamChestButtonLayout.GAP, x)
        assertTrue(x >= recipeBookX + TeamChestButtonLayout.RECIPE_BOOK_WIDTH, "must not overlap the recipe book button")
        assertTrue(
            x + TeamChestButtonLayout.WIDTH <= inventoryWidth,
            "must stay inside the inventory panel",
        )
    }

    @Test
    fun `button shares the recipe book row`() {
        // vanilla InventoryScreen uses height / 2 - 22 for the recipe book button, which is
        // guiTop + 61 for the 166px tall survival inventory
        assertEquals(61, TeamChestButtonLayout.y(0))
        assertEquals(126, TeamChestButtonLayout.x(0))
    }

    @Test
    fun `position follows the container screen offset`() {
        assertEquals(126 + 40, TeamChestButtonLayout.x(40))
        assertEquals(61 + 25, TeamChestButtonLayout.y(25))
    }

    @Test
    fun `button is hidden outside a round, for spectators, and when the chest is off`() {
        assertTrue(TeamChestButtonLayout.shouldShow(isInGame = true, isSpectator = false, teamChestEnabled = true))
        assertFalse(TeamChestButtonLayout.shouldShow(isInGame = false, isSpectator = false, teamChestEnabled = true))
        assertFalse(TeamChestButtonLayout.shouldShow(isInGame = true, isSpectator = true, teamChestEnabled = true))
        assertFalse(TeamChestButtonLayout.shouldShow(isInGame = true, isSpectator = false, teamChestEnabled = false))
    }
}
