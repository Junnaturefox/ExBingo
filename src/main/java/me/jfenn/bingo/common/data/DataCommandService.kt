package me.jfenn.bingo.common.data

import me.jfenn.bingo.common.card.tag.TagData
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.card.tierlist.TierListConfig
import me.jfenn.bingo.platform.IExecutors
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.ReloadEvent
import net.minecraft.server.MinecraftServer
import java.util.concurrent.CompletableFuture

internal class DataCommandService(
    private val scopedData: ScopedData,
    private val tagLoader: TagLoader,
    private val tierListLoader: TierListLoader,
    private val eventBus: IEventBus,
    private val server: MinecraftServer,
    private val executors: IExecutors,
) {
    private fun reload() {
        val reloadEvent = ReloadEvent(server.resourceManager, executors.io, executors.main)
        eventBus.emit(ReloadEvent, reloadEvent)
            .let { CompletableFuture.allOf(*it.toTypedArray()) }
            .thenAcceptAsync({
                eventBus.emit(ReloadEvent.After, ReloadEvent.After())
            }, executors.main)
    }

    fun addToTag(tagName: String, objective: String) = addAllToTag(tagName, listOf(objective))

    /**
     * Writes every objective into the tag with a single write and a single reload.
     *
     * [scopedData] is only refreshed asynchronously by [reload], so calling [addToTag] in a loop
     * re-reads the same stale snapshot each iteration and each write discards the previous one -
     * only the last objective would survive.
     */
    fun addAllToTag(tagName: String, objectives: Collection<String>) {
        if (objectives.isEmpty()) return
        val tag = scopedData.tags[tagName] ?: TagData.EMPTY
        tagLoader.writeTag(tagName, objectives.fold(tag) { acc, objective -> acc.plus(objective) })
        reload()
    }

    fun removeFromTag(tagName: String, objective: String) = removeAllFromTag(tagName, listOf(objective))

    fun removeAllFromTag(tagName: String, objectives: Collection<String>) {
        if (objectives.isEmpty()) return
        val tag = scopedData.tags[tagName] ?: TagData.EMPTY
        tagLoader.writeTag(tagName, objectives.fold(tag) { acc, objective -> acc.minus(objective) })
        reload()
    }

    fun addToTierList(listName: String, objective: String, tier: TierLabel?) =
        addAllToTierList(listName, listOf(objective), tier)

    fun addAllToTierList(listName: String, objectives: Collection<String>, tier: TierLabel?) {
        if (objectives.isEmpty()) return
        val tierList = scopedData.tierLists[listName] ?: TierListConfig.EMPTY
        tierListLoader.writeTierList(listName, objectives.fold(tierList) { acc, objective -> acc.plus(objective, tier) })
        reload()
    }

    fun removeFromTierList(listName: String, objective: String) =
        removeAllFromTierList(listName, listOf(objective))

    fun removeAllFromTierList(listName: String, objectives: Collection<String>) {
        if (objectives.isEmpty()) return
        val tierList = scopedData.tierLists[listName] ?: TierListConfig.EMPTY
        tierListLoader.writeTierList(listName, objectives.fold(tierList) { acc, objective -> acc.minus(objective) })
        reload()
    }
}
