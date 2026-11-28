package me.jfenn.bingo.client.common.teamchest

import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.IKeyBindingManager
import me.jfenn.bingo.client.platform.event.model.ClientTickEvent
import me.jfenn.bingo.common.KEYBIND_OPEN_TEAM_CHEST
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.event.IEventBus
import org.lwjgl.glfw.GLFW

internal class TeamChestKeybindController(
    keyBindingManager: IKeyBindingManager,
    eventBus: IEventBus,
    private val client: IClient,
    private val state: BingoHudState,
    private val text: TextProvider,
) {
    private val openTeamChestKey = keyBindingManager.registerKey(
        KEYBIND_OPEN_TEAM_CHEST,
        GLFW.GLFW_KEY_B,
        StringKey.FullName.key,
    )

    private var lastSpectatorHint: net.minecraft.network.chat.Component? = null

    init {
        eventBus.register(ClientTickEvent.End) {
            if (client.isPaused)
                return@register

            if (state.gameStatus.isInGame && client.player.isSpectator) {
                // only send when the text changes: sending it every tick keeps resetting the
                // overlay timer, which permanently hides every other hotbar message
                val hint = text.string(StringKey.HudSpectatorTeamSelect, openTeamChestKey.displayName)
                if (hint.value != lastSpectatorHint) {
                    lastSpectatorHint = hint.value
                    client.player.sendHotbarMessage(hint)
                }
            } else {
                lastSpectatorHint = null
            }

            if (openTeamChestKey.wasPressed()) {
                client.player.sendCommand("teamchest")
            }
        }
    }
}
