package me.jfenn.bingo.common.spawn

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoundedChunkLoaderTest {

    private val directExecutor = Executor { it.run() }

    @Test
    fun `every chunk is requested exactly once`() {
        val chunks = (1..25).toList()
        val requested = mutableListOf<Int>()
        val issued = mutableListOf<CompletableFuture<*>>()

        BoundedChunkLoader.schedule(chunks, concurrency = 4, executor = directExecutor, issued = issued) { chunk ->
            requested += chunk
            CompletableFuture.completedFuture(null)
        }.join()

        assertEquals(chunks.toSet(), requested.toSet())
        assertEquals(chunks.size, requested.size, "chunks must not be requested twice")
        assertEquals(chunks.size, issued.size, "one future per chunk must be recorded")
    }

    @Test
    fun `no more than the concurrency limit is in flight`() {
        val chunks = (1..40).toList()
        val inFlight = AtomicInteger(0)
        val maxInFlight = AtomicInteger(0)
        val pending = mutableMapOf<Int, CompletableFuture<*>>()

        BoundedChunkLoader.schedule(chunks, concurrency = 3, executor = directExecutor, issued = mutableListOf()) { chunk ->
            val now = inFlight.incrementAndGet()
            maxInFlight.accumulateAndGet(now) { a, b -> maxOf(a, b) }
            val future = CompletableFuture<Void>()
            pending[chunk] = future
            // complete it right away, as the real chunk load eventually would
            inFlight.decrementAndGet()
            future.complete(null)
            future
        }.join()

        assertTrue(maxInFlight.get() <= 3, "max in flight was ${maxInFlight.get()}, expected <= 3")
        assertEquals(chunks.size, pending.size)
    }

    @Test
    fun `an empty chunk list completes immediately`() {
        val issued = mutableListOf<CompletableFuture<*>>()
        val result = BoundedChunkLoader.schedule(emptyList<Int>(), 4, directExecutor, issued) {
            CompletableFuture.completedFuture(null)
        }

        assertTrue(result.isDone)
        assertTrue(issued.isEmpty())
    }

    @Test
    fun `a failing chunk does not prevent the rest from loading`() {
        val chunks = (1..10).toList()
        val requested = mutableListOf<Int>()

        BoundedChunkLoader.schedule<Int>(chunks, concurrency = 2, executor = directExecutor, issued = mutableListOf()) { chunk ->
            requested += chunk
            if (chunk == 5) CompletableFuture.failedFuture<Unit>(IllegalStateException("boom"))
            else CompletableFuture.completedFuture(Unit)
        }

        assertEquals(chunks.toSet(), requested.toSet())
    }
}
