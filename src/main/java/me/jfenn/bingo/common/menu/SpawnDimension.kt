package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.options.listSelectableSpawnDimensions
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IServerWorldFactory
import org.joml.Vector3d

internal const val MENU_DIMENSION_WIDTH = 2.0

internal fun MenuComponent.registerDimensionMenu(
    position: Vector3d,
    width: Double = MENU_DIMENSION_WIDTH,
    height: Double,
    state: BingoState = koinScope.get(),
    serverWorldFactory: IServerWorldFactory = koinScope.get(),
    optionsService: OptionsService = koinScope.get(),
) {
    val dimensions = serverWorldFactory.listSelectableSpawnDimensions()

    var dimension by state.options::spawnDimension
    val indexProp = DelegatedProperty(
        getter = { dimensions.indexOfFirst { it == dimension } },
        setter = { dimension = dimensions[it] },
    )

    val dimensionLabels = dimensions.map { identifier ->
        // Reuse the standard dimension translation key dimension.<namespace>.<path> so the names
        // provided by mods, vanilla, and this mod's resource packs all apply; there is no separate
        // bingo.dimension.* key set to maintain.
        val key = "dimension." + identifier.replace(':', '.').replace('/', '.')
        val fallback = identifier.substringAfterLast(':').formatTitle()
        text.translatable(key, fallback)
    }

    registerRadioMenu(
        position = position,
        width = width,
        height = height,
        title = text.string(StringKey.OptionsSpawnDimension),
        options = dimensionLabels,
        selectedIndexProp = indexProp,
    ) { player, index ->
        optionsService.setSpawnDimension(
            OptionsService.Context(player),
            dimensions[index]
        )
    }
}
