package me.jfenn.bingo.common.mixinstate

import me.jfenn.bingo.platform.event.IEventBus
import net.minecraft.server.MinecraftServer
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Typed, per-server state container for state shared between mixin handlers and the rest of the mod.
 *
 * Mixins cannot take part in dependency injection, so handler objects used to keep their maps in
 * top-level `object` fields. That made them process-global: a second server instance in the same JVM
 * (test fixtures, integrated + dedicated in one dev run, world reloads) shared the same values, and
 * nothing freed them when the server stopped. State now lives here, keyed by the server instance,
 * and is dropped by [ServerMixinState.clear] when that server's bingo scope closes.
 */
class MixinStateStore internal constructor() {

    private val values = ConcurrentHashMap<Class<*>, Any>()

    /** Returns the value registered under [type], creating it with [factory] on first use. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: Class<T>, factory: () -> T): T =
        values.computeIfAbsent(type) { factory() } as T

    /** Returns the value registered under [type], or `null` when nothing has been created yet. */
    fun <T : Any> peek(type: Class<T>): T? = type.cast(values[type])

    fun clear() = values.clear()
}

object ServerMixinState {

    private val stores = ConcurrentHashMap<Any, MixinStateStore>()

    /** The state store of [server], created on first use. */
    fun of(server: MinecraftServer): MixinStateStore = storeFor(server)

    /**
     * Same as [of] but accepts any key. Unit tests cannot build a [MinecraftServer], so they key the
     * registry with a plain object instead.
     */
    internal fun storeFor(key: Any): MixinStateStore = stores.computeIfAbsent(key) { MixinStateStore() }

    /** The store of [server], or `null` when nothing was created (does not create one). */
    fun peek(server: MinecraftServer): MixinStateStore? = stores[server]

    /** Drops every value of [server]'s store. Called when the server's bingo scope closes. */
    fun clear(server: MinecraftServer) {
        stores.remove(server)?.clear()
    }

    /** Drops the whole registry (application close, tests). */
    fun clearAll() {
        stores.values.forEach { it.clear() }
        stores.clear()
    }

    internal fun storeCount(): Int = stores.size
}

/**
 * Movement / chunk-batch / loading signals recorded by [me.jfenn.bingo.mixin.ServerPlayNetworkHandlerMixin].
 *
 * [me.jfenn.bingo.common.timer.WaitUntilLoadedController] gates LOADING -> PLAYING on "a chunk-batch
 * acknowledgement arrived after loading started", so stale timestamps from a previous round must not
 * survive; [me.jfenn.bingo.mixinhandler.ServerPlayNetworkHandlerMixinHandler.markLoadingStarted]
 * clears them for the players it is called for.
 */
class NetworkLoadSignals internal constructor() {
    val lastMovement = ConcurrentHashMap<UUID, Instant>()
    val chunkBatchReceived = ConcurrentHashMap<UUID, Instant>()
    val loadingStarted = ConcurrentHashMap<UUID, Instant>()
}

/** The scoped event bus bound to the vanilla save hook ([me.jfenn.bingo.platform.event.model.ServerEvent.Saved]). */
class ServerSaveBridge internal constructor() {
    @Volatile
    var eventBus: IEventBus? = null
}
