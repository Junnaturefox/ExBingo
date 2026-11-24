package me.jfenn.bingo.client.common.teamchest

/**
 * Geometry and visibility rules for the team chest button in the survival inventory.
 *
 * Vanilla places the recipe book button at `(guiLeft + 104, guiTop + 61)` with a size of 20x18, so
 * the team chest button sits immediately to its right with the same size. Keeping this separate from
 * the widget code makes the numbers testable.
 */
internal object TeamChestButtonLayout {

    const val WIDTH = 20
    const val HEIGHT = 18

    /** Offset of the vanilla recipe book button within the container screen. */
    const val RECIPE_BOOK_OFFSET_X = 104
    const val RECIPE_BOOK_OFFSET_Y = 61
    const val RECIPE_BOOK_WIDTH = 20

    /** Gap between the recipe book button and ours. */
    const val GAP = 2

    fun x(guiLeft: Int): Int = guiLeft + RECIPE_BOOK_OFFSET_X + RECIPE_BOOK_WIDTH + GAP

    fun y(guiTop: Int): Int = guiTop + RECIPE_BOOK_OFFSET_Y

    /**
     * The button is only useful during a running round, for players that can open a survival
     * inventory, and while the team chest is enabled on the server.
     */
    fun shouldShow(isInGame: Boolean, isSpectator: Boolean, teamChestEnabled: Boolean): Boolean =
        isInGame && !isSpectator && teamChestEnabled
}
