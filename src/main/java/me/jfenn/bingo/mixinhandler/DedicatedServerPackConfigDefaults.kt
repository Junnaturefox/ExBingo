package me.jfenn.bingo.mixinhandler

import net.minecraft.server.packs.repository.PackRepository
import net.minecraft.world.level.DataPackConfig
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.flag.FeatureFlags
import org.slf4j.LoggerFactory

object DedicatedServerPackConfigDefaults {
    private const val BUNDLE_FEATURE_PACK_ID = "bundle"
    private const val VANILLA_PACK_ID = "vanilla"

    private val log = LoggerFactory.getLogger("ExBingo")

    @JvmStatic
    fun forceBundleFeaturePack(config: DataPackConfig): DataPackConfig {
        val enabled = config.enabled.filterNot { it == BUNDLE_FEATURE_PACK_ID }.toMutableList()
        val insertIndex = enabled.indexOf(VANILLA_PACK_ID)
            .takeIf { it >= 0 }
            ?.plus(1)
            ?: 0
        enabled.add(insertIndex, BUNDLE_FEATURE_PACK_ID)
        val disabled = config.disabled.filterNot { it == BUNDLE_FEATURE_PACK_ID }
        return DataPackConfig(enabled, disabled)
    }

    @JvmStatic
    fun forceBundleFeaturePack(config: WorldDataConfiguration): WorldDataConfiguration {
        return WorldDataConfiguration(
            forceBundleFeaturePack(config.dataPacks),
            config.enabledFeatures.join(FeatureFlagSet.of(FeatureFlags.BUNDLE)),
        )
    }

    /**
     * Move [packId] into the enabled list (and out of the disabled list) if it is not there yet.
     *
     * This is what makes a requested lobby datapack survive a `level.dat` that lists it as disabled,
     * which is how the server ends up booting without the lobby dimension.
     */
    /**
     * Pure helper: the `(enabled, disabled)` pack lists with [packId] moved into `enabled`.
     *
     * Kept separate from [forcePackEnabled] so it can be unit tested without Minecraft classes.
     */
    fun enablePack(
        enabled: List<String>,
        disabled: List<String>,
        packId: String,
    ): Pair<List<String>, List<String>> {
        if (packId in enabled) return enabled to disabled
        return (enabled + packId) to disabled.filterNot { it == packId }
    }

    @JvmStatic
    fun forcePackEnabled(config: WorldDataConfiguration, packId: String): WorldDataConfiguration {
        if (packId in config.dataPacks.enabled) return config

        val (enabled, disabled) = enablePack(config.dataPacks.enabled, config.dataPacks.disabled, packId)
        return WorldDataConfiguration(
            DataPackConfig(enabled, disabled),
            config.enabledFeatures,
        )
    }

    @JvmStatic
    fun forceBundleFeaturePack(packRepository: PackRepository, config: WorldDataConfiguration): WorldDataConfiguration {
        var adjusted = forceBundleFeaturePack(config)

        LobbyDatapackRequest.packId?.let { lobbyPackId ->
            if (packRepository.isAvailable(lobbyPackId)) {
                val forced = forcePackEnabled(adjusted, lobbyPackId)
                if (forced !== adjusted) {
                    log.warn(
                        "[DedicatedServerPackConfigDefaults] The lobby datapack '{}' was not enabled in this save - forcing it on. " +
                            "It may have been disabled by another datapack manager or by editing level.dat.",
                        lobbyPackId
                    )
                    adjusted = forced
                }
            } else {
                log.error(
                    "[DedicatedServerPackConfigDefaults] Lobby mode is requested for this save, but the datapack '{}' is not available. " +
                        "The lobby dimension will not load - check that the datapack file exists in the save's datapacks folder.",
                    lobbyPackId
                )
            }
        }

        // setSelected() is what actually loads the packs (openAllSelected runs right after this
        // call), so it must run whenever the selection changed - not only when the bundle pack
        // happens to be available.
        if (packRepository.isAvailable(BUNDLE_FEATURE_PACK_ID) || LobbyDatapackRequest.packId != null) {
            packRepository.setSelected(adjusted.dataPacks.enabled)
        }

        return adjusted
    }
}
