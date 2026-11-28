package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.ServerEvent
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory

/**
 * Bridges vanilla's save routine to [ServerEvent.Saved], so the bingo state is written out together
 * with the world (autosave / `/save-all`) instead of only on state changes and shutdown.
 *
 * The scoped event bus is registered by [me.jfenn.bingo.common.state.PersistentStateManager] and
 * cleared when the scope stops.
 */
object ServerEventMixinHandler {

    private val log = LoggerFactory.getLogger("ExBingo")

    @Volatile
    private var eventBus: IEventBus? = null

    fun register(bus: IEventBus) {
        eventBus = bus
    }

    fun unregister() {
        eventBus = null
    }

    @JvmStatic
    fun onSaved(server: MinecraftServer) {
        val bus = eventBus ?: return
        runCatching { bus.emit(ServerEvent.Saved, ServerEvent(server)) }
            .onFailure { log.error("[ServerEventMixinHandler] Error handling ServerEvent.Saved:", it) }
    }
}
