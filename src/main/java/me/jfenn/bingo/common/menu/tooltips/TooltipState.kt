package me.jfenn.bingo.common.menu.tooltips

import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.platform.IEntity
import java.util.*

internal class TooltipState {

    private val registeredTooltips = mutableMapOf<UUID, List<IText>>()

    operator fun set(entity: IEntity, tooltip: List<IText>) {
        registeredTooltips[entity.uuid] = tooltip
    }

    operator fun get(uuid: UUID) = registeredTooltips[uuid]

    /** Drop the tooltip of a despawned menu entity (the map would otherwise grow forever). */
    fun remove(uuid: UUID) {
        registeredTooltips.remove(uuid)
    }

    fun clear() {
        registeredTooltips.clear()
    }

}
