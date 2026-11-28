package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.common.mixinstate.ServerMixinState
import me.jfenn.bingo.common.mixinstate.ServerSaveBridge
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.ServerEvent
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory

/**
 * Bridges vanilla's save routine to [ServerEvent.Saved], so the bingo state is written out together
 * with the world (autosave / `/save-all`) instead of only on state changes and shutdown.
 *
 * The bus is bound to the server it belongs to (see [ServerMixinState]); the scoped event bus is
 * registered by [me.jfenn.bingo.common.state.PersistentStateManager] and released when the scope
 * stops, so a later server instance never emits into a closed bus.
 */
object ServerEventMixinHandler {

    private val log = LoggerFactory.getLogger("ExBingo")

    private fun bridge(server: MinecraftServer): ServerSaveBridge =
        ServerMixinState.of(server).get(ServerSaveBridge::class.java) { ServerSaveBridge() }

    fun register(server: MinecraftServer, bus: IEventBus) {
        bridge(server).eventBus = bus
    }

    fun unregister(server: MinecraftServer) {
        // peek (not of): unregistering must never create a store that nothing will ever clear
        ServerMixinState.peek(server)?.peek(ServerSaveBridge::class.java)?.eventBus = null
    }

    @JvmStatic
    fun onSaved(server: MinecraftServer) {
        val bus = ServerMixinState.peek(server)?.peek(ServerSaveBridge::class.java)?.eventBus ?: return
        runCatching { bus.emit(ServerEvent.Saved, ServerEvent(server)) }
            .onFailure { log.error("[ServerEventMixinHandler] Error handling ServerEvent.Saved:", it) }
    }
}
