package me.jfenn.bingo.common.card.tag

import kotlinx.serialization.SerialName
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.filter.ObjectiveFilter
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import kotlin.random.Random
import kotlin.reflect.full.findAnnotation

class ObjectiveTagProvider(
    private val objectiveManager: BingoObjectiveManager,
) : ITagProvider {
    override fun listTags(): Map<String, TagData> {
        val types = objectiveManager.list()
            .mapNotNull { objectiveManager.find(it, CardGeneratorState(Random.Default)) }
            // the objective type comes from the @SerialName annotation on the data class; there is no
            // explicit type field in the model, so reflection on the annotation is the only source.
            .groupBy { it::class.findAnnotation<SerialName>()?.value }
            .mapNotNull { (key, value) ->
                key?.let {
                    ObjectiveFilter.type(key) to TagData(
                        value.map { it.id }.toSet()
                    )
                }
            }
            .toMap()

        val namespaces = objectiveManager.list()
            .groupBy { ObjectiveFilter.from(it.substringBefore(':')) }
            .mapValues { (_, value) -> TagData(value.toSet()) }

        return types + namespaces
    }
}
