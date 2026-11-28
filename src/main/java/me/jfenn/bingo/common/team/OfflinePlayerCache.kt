package me.jfenn.bingo.common.team

import me.jfenn.bingo.common.utils.measureTime
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.GameResetEvent
import me.jfenn.bingo.platform.player.PlayerProfile
import org.slf4j.Logger
import java.lang.ref.SoftReference

class OfflinePlayerCache(
    eventBus: IEventBus,
    private val playerManager: IPlayerManager,
    private val log: Logger,
) {

    /**
     * Cached handles hold a full offline player graph (inventory, advancements, the level they
     * were created in), so they are only softly reachable: a long-running server that has seen many
     * players would otherwise never release any of them.
     */
    private val offlinePlayerCache = mutableMapOf<PlayerProfile, SoftReference<IPlayerHandle>>()

    init {
        // A reset recreates the ServerLevels, which would leave every cached handle pointing at a
        // closed world, so drop the entries outright when the game is reset.
        eventBus.register(GameResetEvent) {
            offlinePlayerCache.clear()
        }
    }

    fun getOfflinePlayer(profile: PlayerProfile): IPlayerHandle {
        // If the player is online, return the current player entity
        val onlinePlayer = playerManager.getPlayer(profile.uuid)
        if (onlinePlayer != null) {
            // and set their offline cache entry
            offlinePlayerCache[profile] = SoftReference(onlinePlayer)
            return onlinePlayer
        }

        // If the player is in the offline cache, return it
        offlinePlayerCache[profile]?.get()?.let { return it }

        // Otherwise, load their player data from the save (this can take a few ms...)
        val player: IPlayerHandle = log.measureTime("Loading offline player data for ${profile.name}") {
            playerManager.getOfflinePlayer(profile)
        }

        offlinePlayerCache[profile] = SoftReference(player)
        return player
    }

}
