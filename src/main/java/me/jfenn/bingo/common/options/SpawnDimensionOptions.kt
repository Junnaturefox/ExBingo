package me.jfenn.bingo.common.options

import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.platform.IServerWorldFactory

internal fun IServerWorldFactory.listSelectableSpawnDimensions(): List<String> {
    return listWorlds()
        .map { it.identifier }
        .filterNot { it == LOBBY_WORLD_ID.toString() }
        .sorted()
}

internal fun IServerWorldFactory.coerceSpawnDimension(dimension: String): String {
    val selectable = listSelectableSpawnDimensions()
    return dimension.takeIf { it in selectable } ?: "minecraft:overworld"
}
