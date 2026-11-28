package me.jfenn.bingo.common.card.tag

import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.filter.ObjectiveFilter
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import me.jfenn.bingo.common.data.ScopedData
import org.slf4j.Logger
import kotlin.random.Random

internal class UnobtainableTagProvider(
    private val data: ScopedData,
    private val objectiveManager: BingoObjectiveManager,
    private val log: Logger,
): ITagProvider {
    override fun listTags(): Map<String, TagData> {
        val unobtainableItems = objectiveManager.list()
            .asSequence()
            // ignore items that are manually categorized to a tier list or tag
            .filterNot { obj -> data.tierLists.values.any { it.isCategorized(obj) } }
            .filterNot { obj -> data.tags.values.any { it.contains(obj) } }
            // get an item instance from the id, if it exists
            .mapNotNull {
                objectiveManager.find(it, CardGeneratorState(Random.Default))
            }
            .filterIsInstance<BingoObjective.ItemEntry>()
            // itemStack should already be initialized if found
            .filterNot {
                val id = it.itemStack?.identifier?.toString()
                if (id == null) {
                    // "not unobtainable" keeps the item on cards; losing an entry here would
                    // silently remove it from every board
                    log.warn("[UnobtainableTagProvider] Objective {} has no item stack; keeping it on cards", it.id)
                    return@filterNot true
                }
                data.obtainableItems.contains(id)
            }
            .map { it.id }
            .toSet()
            .also {
                log.info("[UnobtainableTagProvider] Excluding ${it.size} unobtainable items")
            }

        return mapOf(ObjectiveFilter.UNOBTAINABLE to TagData(unobtainableItems))
    }

}
