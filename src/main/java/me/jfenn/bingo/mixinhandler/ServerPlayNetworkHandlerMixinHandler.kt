package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.common.mixinstate.NetworkLoadSignals
import me.jfenn.bingo.common.mixinstate.ServerMixinState
import net.minecraft.server.level.ServerPlayer
import java.time.Instant

/**
 * Records player movement and chunk-batch acknowledgements so the server can tell when the client
 * has actually received its spawn terrain.
 *
 * The signals live in a per-server store (see [ServerMixinState]) rather than in process-global
 * maps: two server instances in one JVM must not share them, and they have to be dropped with the
 * server's bingo scope.
 */
object ServerPlayNetworkHandlerMixinHandler {

    private fun signals(player: ServerPlayer): NetworkLoadSignals =
        ServerMixinState.of(player.serverLevel().server)
            .get(NetworkLoadSignals::class.java) { NetworkLoadSignals() }

    fun getLastPlayerMovement(player: ServerPlayer): Instant? =
        signals(player).lastMovement[player.uuid]

    fun onPlayerMove(player: ServerPlayer) {
        signals(player).lastMovement[player.uuid] = Instant.now()
    }

    /**
     * The last time the client acknowledged receiving a batch of chunk data. This is a direct signal
     * that terrain has arrived on the client, used to gate LOADING -> PLAYING.
     */
    fun getLastChunkBatchReceived(player: ServerPlayer): Instant? =
        signals(player).chunkBatchReceived[player.uuid]

    fun onChunkBatchReceived(player: ServerPlayer) {
        signals(player).chunkBatchReceived[player.uuid] = Instant.now()
    }

    fun getLoadingStarted(player: ServerPlayer): Instant? =
        signals(player).loadingStarted[player.uuid]

    fun markLoadingStarted(player: ServerPlayer) {
        markLoadingStarted(listOf(player))
    }

    /** Drop the recorded signals for a player (e.g. on logout), so nothing outlives the session. */
    fun remove(player: ServerPlayer) {
        val signals = signals(player)
        signals.lastMovement.remove(player.uuid)
        signals.chunkBatchReceived.remove(player.uuid)
        signals.loadingStarted.remove(player.uuid)
    }

    /**
     * Clear the recorded movement / chunk-batch timestamps for the given players and remember when
     * their current terrain-loading attempt began.
     *
     * A stale chunk-batch ack from the lobby or a previous round can satisfy "terrain arrived after
     * loading started" before the fresh spawn terrain was actually sent, dropping the client into an
     * unloaded world (void / can't break blocks). Resetting the signals whenever a new LOADING phase
     * begins - and again after a recovery teleport is sent - forces the gate to wait for a genuinely
     * new acknowledgement.
     */
    fun markLoadingStarted(players: Iterable<ServerPlayer>) {
        val now = Instant.now()
        for (player in players) {
            val signals = signals(player)
            signals.lastMovement.remove(player.uuid)
            signals.chunkBatchReceived.remove(player.uuid)
            signals.loadingStarted[player.uuid] = now
        }
    }
}
