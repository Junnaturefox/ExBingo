package me.jfenn.bingo.client.common.teamchest

import me.jfenn.bingo.client.common.config.ClientServerConfigState
import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.event.model.ClientTickEvent
import me.jfenn.bingo.client.platform.event.model.ScreenEvent
import me.jfenn.bingo.client.platform.screen.IButton
import me.jfenn.bingo.client.platform.screen.IButtonFactory
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.event.IEventBus
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import org.joml.Vector2i

/**
 * Adds a team chest button to the survival inventory, next to the vanilla recipe book button.
 *
 * Opening the team chest used to require typing `/tc`, which is awkward for players who do not know
 * the command. The button is the same size as the recipe book button, uses a copper chest icon, and
 * simply sends the team chest command when pressed - exactly what typing it would do.
 *
 * It is only added while a round is running ([BingoHudState.gameStatus]) and while the server has the
 * team chest enabled, so a normal inventory looks untouched outside of a game. The position is
 * refreshed every tick from the container screen's `guiLeft`/`guiTop`, because toggling the recipe
 * book shifts the whole inventory sideways without re-initialising the screen.
 */
internal class TeamChestInventoryButtonController(
    private val client: IClient,
    eventBus: IEventBus,
    private val buttonFactory: IButtonFactory,
    private val state: BingoHudState,
    private val text: TextProvider,
) : BingoComponent() {

    private var button: IButton? = null
    private var inventory: InventoryScreen? = null

    init {
        eventBus.register(ScreenEvent.AfterInit) { event ->
            val screen = event.screen.screen as? InventoryScreen ?: return@register
            clear()

            if (!shouldShowButton()) return@register

            val newButton = buttonFactory.createButton(
                x = 0,
                y = 0,
                width = TeamChestButtonLayout.WIDTH,
                height = TeamChestButtonLayout.HEIGHT,
                texture = TEXTURE,
                focusedTexture = TEXTURE_FOCUSED,
                onClick = { client.player.sendCommand(TEAM_CHEST_COMMAND) },
                message = text.string(StringKey.TeamchestButton),
            )
            event.screen.addButton(newButton)

            button = newButton
            inventory = screen
            updatePosition(screen)
        }

        eventBus.register(ClientTickEvent.End) {
            val screen = inventory ?: return@register

            // the screen is re-initialised on resize and closed on escape, so stop tracking it then
            if (client.screen !== screen) {
                clear()
                return@register
            }

            updatePosition(screen)
        }
    }

    private fun shouldShowButton(): Boolean {
        val teamChestEnabled = ClientServerConfigState.snapshot?.config?.teamChestEnabled ?: true
        return TeamChestButtonLayout.shouldShow(
            isInGame = state.gameStatus.isInGame,
            isSpectator = client.player.isSpectator,
            teamChestEnabled = teamChestEnabled,
        )
    }

    private fun updatePosition(screen: InventoryScreen) {
        val button = button ?: return
        button.position = Vector2i(
            TeamChestButtonLayout.x(screen.guiLeft),
            TeamChestButtonLayout.y(screen.guiTop),
        )
    }

    private fun clear() {
        button = null
        inventory = null
    }

    private companion object {
        const val TEXTURE = "minecraft:bingo/team_chest"
        const val TEXTURE_FOCUSED = "minecraft:bingo/team_chest_highlighted"

        /** same command the team chest keybind sends; `/tc` is registered as an alias of it */
        const val TEAM_CHEST_COMMAND = "teamchest"
    }
}
