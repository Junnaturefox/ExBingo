package me.jfenn.bingo.api.event

import me.jfenn.bingo.api.annotations.BingoInternalApi
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

class EventListener<T>(
    private val label: String? = null,
) {
    // Copy-on-write so a handler may unregister itself (or register another handler) while the
    // event is being dispatched without a ConcurrentModificationException skipping later listeners.
    private val callbacks = CopyOnWriteArrayList<Consumer<T>>()
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun register(callback: Consumer<T>): Handle {
        callbacks.add(callback)
        return Handle(callback)
    }

    inner class Handle(
        var callback: Consumer<T>?,
    ) : Closeable {
        override fun close() {
            // idempotent: closing twice must not remove another listener's entry
            val cb = callback ?: return
            callback = null
            callbacks.remove(cb)
        }
    }

    @BingoInternalApi
    fun invoke(event: T) {
        for (callback in callbacks) {
            try {
                callback.accept(event)
            } catch (e: Throwable) {
                logger.error("Exception in event handler ($label):", e)
            }
        }
    }
}
