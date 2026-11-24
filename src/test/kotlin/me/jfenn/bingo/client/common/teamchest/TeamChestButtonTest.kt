package me.jfenn.bingo.client.common.teamchest

import kotlin.test.Test
import kotlin.test.assertEquals

class TeamChestButtonTest {

    /**
     * Regression test for a StackOverflowError that crashed the client on the first click: the
     * click action used to be named `onPress`, so calling `onPress()` inside the override resolved
     * to the override itself instead of the action.
     */
    @Test
    fun `pressing the button runs the click action once`() {
        var presses = 0
        val button = TeamChestButton(0, 0) { presses++ }

        button.onPress()

        assertEquals(1, presses)
    }
}
