package me.jfenn.bingo.common.menu.lifecycle

import java.util.UUID

/**
 * Tag helpers for the entities that make up a lobby menu instance.
 *
 * Every menu entity is tagged with the tag of the instance that spawned it. When the menu is rebuilt
 * (or a world is reloaded) the previous instance's entities are still in the world, and they must be
 * swept - otherwise they swallow clicks and keep rendering. The "is this tag from another instance"
 * rule was duplicated in `MenuController` and `MenuInstance`; it lives here so both sides and the
 * tests agree on it.
 */
internal const val LOBBY_TAG_PREFIX = "bingo-"

/** A fresh tag that identifies one menu instance. */
internal fun newLobbyInstanceTag(): String = LOBBY_TAG_PREFIX + UUID.randomUUID()

/** Every bingo lobby tag carried by this entity. */
internal fun Collection<String>.lobbyTags(): List<String> = filter { it.startsWith(LOBBY_TAG_PREFIX) }

/** True when the entity was tagged by the running menu instance. */
internal fun Collection<String>.isLobbyEntityOf(instanceTag: String): Boolean = instanceTag in lobbyTags()

/**
 * True when the entity belongs to a *different* lobby instance and should be swept.
 *
 * Entities without any bingo tag are not menu entities at all and are never stale, so this returns
 * false for them.
 */
internal fun Collection<String>.isStaleLobbyEntity(instanceTag: String): Boolean {
    val tags = lobbyTags()
    return tags.isNotEmpty() && instanceTag !in tags
}
