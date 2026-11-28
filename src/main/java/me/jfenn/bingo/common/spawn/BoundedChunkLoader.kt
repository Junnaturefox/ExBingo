package me.jfenn.bingo.common.spawn

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.math.max
import kotlin.math.min

/**
 * Loads a list of chunks through a bounded number of sequential chains.
 *
 * Requesting every spawn-area chunk at once queues hundreds of full-status generations, which
 * spikes the chunk worker queue and the heap exactly when a round starts. Sequential chains keep at
 * most [concurrency] requests in flight while still overlapping several per core.
 */
internal object BoundedChunkLoader {

    /**
     * @param issued collects one future per requested chunk (for progress reporting).
     * @param load issues the request for a single chunk; it is always invoked through [executor].
     * @return a future that completes once every chunk has loaded.
     */
    fun <T> schedule(
        chunks: List<T>,
        concurrency: Int,
        executor: Executor,
        issued: MutableList<CompletableFuture<*>>,
        load: (T) -> CompletableFuture<*>,
    ): CompletableFuture<Void> {
        if (chunks.isEmpty()) return CompletableFuture.completedFuture(null)

        val workerCount = min(max(concurrency, 1), chunks.size)
        val chains = (0 until workerCount).map { worker ->
            var chain: CompletableFuture<*> = CompletableFuture.completedFuture(null)
            for (index in worker until chunks.size step workerCount) {
                val chunk = chunks[index]
                val request = chain.thenComposeAsync({
                    load(chunk).also { issued += it }
                }, executor)
                // a single failed chunk must not stop the remaining chunks of this chain from
                // being requested - the round can start with a few chunks left to generate
                chain = request.exceptionally { null }
            }
            chain
        }

        return CompletableFuture.allOf(*chains.toTypedArray())
    }
}
