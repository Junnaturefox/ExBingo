package me.jfenn.bingo.mixinhandler

import net.minecraft.server.level.ServerPlayer
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ServerPlayNetworkHandlerMixinHandler {

    private val playerMap = ConcurrentHashMap<UUID, Instant>()
    private val chunkBatchMap = ConcurrentHashMap<UUID, Instant>()
    private val loadingStartMap = ConcurrentHashMap<UUID, Instant>()

    fun getLastPlayerMovement(player: ServerPlayer): Instant? {
        val id = player.uuid
        return playerMap[id]
    }

    fun onPlayerMove(player: ServerPlayer) {
        val id = player.uuid
        playerMap[id] = Instant.now()
    }

    /**
     * The last time the client acknowledged receiving a batch of chunk data. This is a
     * direct signal that terrain has arrived on the client, used to gate LOADING -> PLAYING.
     */
    fun getLastChunkBatchReceived(player: ServerPlayer): Instant? {
        val id = player.uuid
        return chunkBatchMap[id]
    }

    fun onChunkBatchReceived(player: ServerPlayer) {
        val id = player.uuid
        chunkBatchMap[id] = Instant.now()
    }

    fun getLoadingStarted(player: ServerPlayer): Instant? {
        val id = player.uuid
        return loadingStartMap[id]
    }

    fun markLoadingStarted(player: ServerPlayer) {
        markLoadingStarted(listOf(player))
    }

    /**
     * Clear the recorded movement / chunk-batch timestamps for the given players and remember
     * when their current terrain-loading attempt began.
     *
     * These maps are never pruned on their own and are keyed by the player's identity hash, so
     * timestamps recorded in a previous round (or while the player was idling in the lobby) can
     * survive into the next round. [WaitUntilLoadedController] gates LOADING -> PLAYING on
     * "a chunk-batch ack arrived AFTER loading started"; a stale timestamp left over from the
     * lobby/previous game can satisfy that check before the fresh spawn terrain has actually been
     * sent, dropping the client into an unloaded world (void / can't break blocks). Resetting the
     * signals when a new LOADING phase begins, and again after a recovery teleport is sent,
     * forces the gate to wait for a genuinely new ack.
     */
    /** Drop the recorded signals for a player (e.g. on logout), so nothing outlives the session. */
    fun remove(player: ServerPlayer) {
        playerMap.remove(player.uuid)
        chunkBatchMap.remove(player.uuid)
        loadingStartMap.remove(player.uuid)
    }

    fun markLoadingStarted(players: Iterable<ServerPlayer>) {
        val now = Instant.now()
        for (player in players) {
            val id = player.uuid
            playerMap.remove(id)
            chunkBatchMap.remove(id)
            loadingStartMap[id] = now
        }
    }
}
