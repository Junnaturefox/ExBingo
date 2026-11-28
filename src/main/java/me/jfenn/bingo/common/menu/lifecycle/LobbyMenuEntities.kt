package me.jfenn.bingo.common.menu.lifecycle

import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.platform.EntityType
import me.jfenn.bingo.platform.IEntity
import me.jfenn.bingo.platform.IEntityManager
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TicketType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import org.joml.Vector3d
import org.slf4j.Logger

internal data class MenuEntityStats(
    val current: Int = 0,
    val stale: Int = 0,
) {
    val total: Int get() = current + stale

    override fun toString(): String {
        return "total=$total,current=$current,stale=$stale"
    }
}

/**
 * Bookkeeping for the entities that make up a lobby menu instance.
 *
 * The lobby menu is built out of real entities, so it has a lifecycle of its own: every entity is
 * tagged with the current [instanceTag], entities from previous instances are swept, and the chunks
 * held open for the team picker are released again. Originally this lived inline in `MenuController`
 * mixed with the menu building code; it is isolated here so the "who owns this entity" rules are in
 * one place (and unit-testable through [lobbyTags] / [isStaleLobbyEntity]).
 *
 * All methods run on the server thread.
 */
internal class LobbyMenuEntities(
    private val log: Logger,
    private val entityManager: IEntityManager,
    private val interactionEntityEvents: InteractionEntityEvents,
) {

    /** Tag of the current menu instance; rotated every time the lobby is spawned. */
    var instanceTag: String = newLobbyInstanceTag()
        private set

    /**
     * Ticks remaining for the post-spawn stale-entity sweep. Persisted entities from a previous
     * lobby instance can drain out of the async loading inbox several ticks after spawn, so the
     * one-shot cleanup is not enough - see [sweepStale].
     */
    var staleSweepTicksRemaining: Int = 0

    private val teamPickerChunks = mutableSetOf<ChunkPos>()

    fun rotateInstanceTag() {
        instanceTag = newLobbyInstanceTag()
    }

    fun stats(world: ServerLevel): MenuEntityStats {
        var current = 0
        var stale = 0
        world.allEntities.forEach { entity ->
            if (entity is Player) return@forEach
            val bingoTags = entity.tags.lobbyTags()
            if (bingoTags.isEmpty()) return@forEach
            if (instanceTag in bingoTags) current++ else stale++
        }
        return MenuEntityStats(current = current, stale = stale)
    }

    /**
     * Discards any lobby entity carrying a bingo- tag that is NOT the current [instanceTag].
     *
     * Their interaction listeners are already gone, so leaving them in place steals clicks from the
     * real menu buttons they overlap. The caller owns the "only sweep during PREGAME, and only while
     * [staleSweepTicksRemaining] is positive" gate.
     */
    fun sweepStale(world: ServerLevel) {
        // snapshot first: allEntities is a live view over the entity lookup, and discarding an
        // entity while iterating it can throw ConcurrentModificationException
        for (entity in world.allEntities.toList()) {
            if (entity is Player) continue
            if (!entity.tags.isStaleLobbyEntity(instanceTag)) continue
            log.info("[MenuController] Sweeping stale lobby entity {} (tags={})", entity.uuid, entity.tags.lobbyTags())
            discard(entity)
        }
    }

    /** Discards every menu entity in [world] (used when the lobby is torn down). */
    fun discardAll(world: ServerLevel) {
        world.allEntities
            .filter { it !is Player && it.tags.any { tag -> tag.startsWith(LOBBY_TAG_PREFIX) } }
            .forEach(::discard)
    }

    fun discard(entity: Entity) {
        interactionEntityEvents.removeInteract(entity.uuid)
        entity.discard()
    }

    /** Platform entity variant, used for [entityManager] iteration results. */
    fun discardPlatformEntity(entity: IEntity) {
        interactionEntityEvents.removeInteract(entity.uuid)
        entity.discard()
    }

    fun trackTeamPickerChunk(pos: ChunkPos) {
        teamPickerChunks += pos
    }

    /** Drops the tracked chunk positions without releasing them (the world is already gone). */
    fun forgetTeamPickerChunks() {
        teamPickerChunks.clear()
    }

    fun releaseTeamPickerTickets(world: ServerLevel) {
        for (chunkPos in teamPickerChunks) {
            world.chunkSource.removeRegionTicket(TEAM_PICKER_TICKET, chunkPos, 0, chunkPos)
        }
        teamPickerChunks.clear()
    }

    /**
     * Removes stale team-picker entities left at [position] by a previous lobby instance. The
     * positions are the three team-picker slots relative to the picker anchor.
     */
    fun cleanupTeamPickerEntities(world: ServerLevel, position: Vector3d) {
        val positions = listOf(
            position + Vector3d(0.0, 1.5, 0.0),
            position + Vector3d(0.0, 1.0, 0.0),
            position + Vector3d(0.0, -1.0, 0.0),
        )

        entityManager.iterateEntities(world)
            .filter { isLobbyEntityType(it.type) }
            .filter { entity -> positions.any { it.isNear(entity.pos) } }
            .filter { it.commandTags.isForeignLobbyEntity(instanceTag) }
            .forEach(::discardPlatformEntity)
    }

    private fun isLobbyEntityType(type: EntityType<*>): Boolean {
        return type == EntityType.TEXT_DISPLAY ||
            type == EntityType.BLOCK_DISPLAY ||
            type == EntityType.INTERACTION
    }

    private fun Vector3d.isNear(other: Vector3d): Boolean {
        return distanceSquared(other) < 1.0E-4
    }

    internal companion object {
        /** Keeps a chunk loaded while the team picker is shown. */
        val TEAM_PICKER_TICKET: TicketType<ChunkPos> = TicketType.create("bingo-team-picker") { a, b ->
            a.toLong().compareTo(b.toLong())
        }
    }
}
