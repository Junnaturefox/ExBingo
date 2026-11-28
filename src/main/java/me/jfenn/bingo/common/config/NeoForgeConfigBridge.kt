package me.jfenn.bingo.common.config

import kotlinx.serialization.encodeToString
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.autotier.AutoTierConfig
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.utils.decodeFromUtf8Stream
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.mixinhandler.ExperienceBottleXpHelper
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.common.ModConfigSpec
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.function.Predicate
import java.util.function.Supplier
import kotlin.io.path.exists

object NeoForgeConfigBridge {
    const val COMMON_FILE_NAME = "$MOD_ID_BINGO-common.toml"
    const val CLIENT_FILE_NAME = "$MOD_ID_BINGO-client.toml"

    private val log = LoggerFactory.getLogger("ExBingo")
    private val legacyConfigRelativePath = Path.of(MOD_ID_BINGO, "config.json")

    @Volatile
    private var startupConfigLoadComplete = false

    /**
     * Set while we are writing to the NeoForge config specs.
     *
     * [ModConfigSpec.save] re-dispatches [ModConfigEvent.Reloading] synchronously on the calling
     * thread (LoadedConfig#save -> ModContainer#acceptEvent -> EventBus#post), so a save triggered
     * from [onConfigEvent] re-enters [onConfigEvent] -> syncReloadedSpec -> save -> ... until the
     * stack overflows (crash: StackOverflowError in NeoForgeConfigBridge.onConfigEvent, which then
     * cascades into ExceptionInInitializerError while FML builds the crash report).
     * Reloading events fired by our own save are already in sync, so they are ignored.
     */
    private val configSyncInProgress: java.lang.ThreadLocal<Boolean> = java.lang.ThreadLocal.withInitial { false }

    private val commonPair = ModConfigSpec.Builder().configure(::CommonValues)
    private val clientPair = ModConfigSpec.Builder().configure(::ClientValues)

    val commonValues: CommonValues = commonPair.left
    val commonSpec: ModConfigSpec = commonPair.right
    val clientValues: ClientValues = clientPair.left
    val clientSpec: ModConfigSpec = clientPair.right

    val mirroredConfigPaths: Set<String> =
        (commonValues.entries + clientValues.entries).map { it.configPath }.toSet()

    val intentionallyJsonOnlyConfigPaths: Map<String, String> = mapOf(
        "version" to "Internal migration marker; it is managed by MigrationHandler.",
        "server.isLobbyModeWorld" to "Save binding for server.isLobbyMode; written by /bingo lobby confirm and consumed at startup, so it must not be editable from the config screen.",
        "server.performanceCleanup.enabled" to "Long-game cleanup is an operational server tuning knob kept in config/exbingo/config.json.",
        "server.performanceCleanup.intervalTicks" to "Long-game cleanup is an operational server tuning knob kept in config/exbingo/config.json.",
        "server.performanceCleanup.minTotalEntities" to "Long-game cleanup is an operational server tuning knob kept in config/exbingo/config.json.",
        "server.performanceCleanup.keepChunkRadius" to "Long-game cleanup is an operational server tuning knob kept in config/exbingo/config.json.",
        "server.performanceCleanup.maxEntitiesPerPass" to "Long-game cleanup is an operational server tuning knob kept in config/exbingo/config.json.",
        "server.performanceCleanup.cleanupItems" to "Long-game cleanup is an operational server tuning knob kept in config/exbingo/config.json.",
        "server.performanceCleanup.cleanupProjectiles" to "Long-game cleanup is an operational server tuning knob kept in config/exbingo/config.json.",
        "server.performanceCleanup.cleanupMobs" to "Long-game cleanup is an operational server tuning knob kept in config/exbingo/config.json.",
        "server.performanceCleanup.cleanupMultipartEntities" to "Long-game cleanup is an operational server tuning knob kept in config/exbingo/config.json.",
        "server.performanceCleanup.protectedEntityNamespaces" to "Long-game cleanup is an operational server tuning knob kept in config/exbingo/config.json.",
    )

    val requiredTranslationKeys: Set<String> =
        buildSet {
            add("$MOD_ID_BINGO.configuration.title")
            add("$MOD_ID_BINGO.configuration.section.exbingo.common.toml")
            add("$MOD_ID_BINGO.configuration.section.exbingo.common.toml.title")
            add("$MOD_ID_BINGO.configuration.section.exbingo.client.toml")
            add("$MOD_ID_BINGO.configuration.section.exbingo.client.toml.title")

            (commonValues.sections + clientValues.sections).forEach {
                add(it.translationKey)
                add("${it.translationKey}.tooltip")
            }

            (commonValues.entries + clientValues.entries).forEach {
                add(it.translationKey)
                add("${it.translationKey}.tooltip")
            }

            add("$MOD_ID_BINGO.configuration.common.auto_tier.mapping.quantile")
            add("$MOD_ID_BINGO.configuration.common.auto_tier.mapping.threshold")
        }

    fun registerConfigs(modContainer: ModContainer, modEventBus: IEventBus) {
        modContainer.registerConfig(ModConfig.Type.COMMON, commonSpec, COMMON_FILE_NAME)
        modContainer.registerConfig(ModConfig.Type.CLIENT, clientSpec, CLIENT_FILE_NAME)
        modEventBus.addListener(ModConfigEvent.Loading::class.java, this::onConfigEvent)
        modEventBus.addListener(ModConfigEvent.Reloading::class.java, this::onConfigEvent)
        modEventBus.addListener(FMLLoadCompleteEvent::class.java) { _: FMLLoadCompleteEvent ->
            markStartupConfigLoadComplete()
        }
    }

    fun onConfigEvent(event: ModConfigEvent) {
        val config = event.config
        val scope = configScopeForFileName(config.fileName)
        if (config.modId != MOD_ID_BINGO || scope == null) {
            return
        }

        if (configSyncInProgress.get()) {
            // Nested Reloading raised by our own ModConfigSpec#save(); the values are already synced.
            return
        }

        configSyncInProgress.set(true)
        try {
            runCatching {
                when (event) {
                    is ModConfigEvent.Loading -> syncInitialLoadedSpec(FMLPaths.CONFIGDIR.get(), scope)
                    is ModConfigEvent.Reloading -> syncReloadedSpec(FMLPaths.CONFIGDIR.get(), scope)
                }
            }.onFailure {
                log.error("Unable to sync ExBingo NeoForge config values with config/exbingo/config.json", it)
            }
        } finally {
            configSyncInProgress.set(false)
        }
    }

    fun applyLoadedSpecs(config: BingoConfig, configDir: Path = FMLPaths.CONFIGDIR.get()): BingoConfig {
        var result = config
        if (commonSpec.isLoaded && loadedSpecShouldOverrideLegacy(configDir, ConfigScope.COMMON)) {
            result = commonValues.applyTo(result)
        }
        if (clientSpec.isLoaded && loadedSpecShouldOverrideLegacy(configDir, ConfigScope.CLIENT)) {
            result = result.copy(client = clientValues.applyTo(result.client))
        }
        return result
    }

    fun updateLoadedSpecsFrom(config: BingoConfig) {
        var saveCommon = false
        var saveClient = false

        if (commonSpec.isLoaded) {
            commonValues.setFrom(config)
            saveCommon = true
        }

        if (clientSpec.isLoaded) {
            clientValues.setFrom(config.client)
            saveClient = true
        }

        if (saveCommon || saveClient) {
            // save() re-dispatches ModConfigEvent.Reloading; hold the guard so writing config.json
            // cannot feed itself back through onConfigEvent -> syncReloadedSpec -> save.
            val alreadySyncing = configSyncInProgress.get()
            if (!alreadySyncing) configSyncInProgress.set(true)
            try {
                if (saveCommon) commonSpec.save()
                if (saveClient) clientSpec.save()
            } finally {
                if (!alreadySyncing) configSyncInProgress.set(false)
            }
        }
    }

    fun snapshotCommonValues(base: BingoConfig = BingoConfig()): BingoConfig =
        commonValues.applyTo(base)

    fun setCommonValuesFrom(config: BingoConfig) {
        commonValues.setFrom(config)
    }

    fun applyCommonValuesTo(config: BingoConfig): BingoConfig =
        commonValues.applyTo(config)

    private fun syncInitialLoadedSpec(configDir: Path, scope: ConfigScope) {
        val source = chooseStartupConfigSource(legacyConfigExists(configDir))
        syncLoadedSpecFromSource(configDir, scope, source)
    }

    private fun syncReloadedSpec(configDir: Path, scope: ConfigScope) {
        val source = chooseReloadConfigSource(
            legacyJsonExists = legacyConfigExists(configDir),
            startupConfigLoadComplete = startupConfigLoadComplete,
        )
        syncLoadedSpecFromSource(configDir, scope, source)
    }

    private fun syncLoadedSpecFromSource(configDir: Path, scope: ConfigScope, source: StartupConfigSource) {
        when (source) {
            StartupConfigSource.LEGACY_JSON -> syncJsonToLoadedSpec(configDir, scope)
            StartupConfigSource.NEOFORGE_TOML -> syncLoadedSpecToJson(configDir, scope)
        }
    }

    private fun syncLoadedSpecToJson(configDir: Path, scope: ConfigScope) {
        val current = readLegacyConfig(configDir)
        val updated = applyLoadedSpec(current, scope)
        ExperienceBottleXpHelper.updateFrom(updated)
        writeLegacyConfig(configDir, updated)
    }

    private fun syncJsonToLoadedSpec(configDir: Path, scope: ConfigScope) {
        val current = readLegacyConfig(configDir)
        ExperienceBottleXpHelper.updateFrom(current)
        val before = snapshotLoadedSpec(scope)
        setLoadedSpecFrom(current, scope)
        val after = snapshotLoadedSpec(scope)
        // Persist only when the spec really changed: save() both rewrites the TOML file and
        // re-dispatches Reloading, so an unconditional save would churn on every reload.
        if (before == null || after == null || before != after) {
            saveLoadedSpec(scope)
        }
    }

    private fun snapshotLoadedSpec(scope: ConfigScope): BingoConfig? =
        runCatching { applyLoadedSpec(BingoConfig(), scope) }.getOrNull()

    private fun readLegacyConfig(configDir: Path): BingoConfig {
        val path = configDir.resolve(legacyConfigRelativePath)
        if (!path.exists()) {
            return BingoConfig()
        }

        return runCatching {
            Files.newInputStream(path).use { json.decodeFromUtf8Stream<BingoConfig>(it) }
        }.onFailure {
            log.error("Unable to read ExBingo config from {}", path, it)
        }.getOrElse { BingoConfig() }
    }

    private fun writeLegacyConfig(configDir: Path, config: BingoConfig) {
        val path = configDir.resolve(legacyConfigRelativePath)
        Files.createDirectories(path.parent)
        Files.newBufferedWriter(path).use {
            it.write(json.encodeToString(config))
        }
    }

    private fun applyLoadedSpec(config: BingoConfig, scope: ConfigScope): BingoConfig =
        when (scope) {
            ConfigScope.COMMON -> commonValues.applyTo(config)
            ConfigScope.CLIENT -> config.copy(client = clientValues.applyTo(config.client))
        }

    private fun setLoadedSpecFrom(config: BingoConfig, scope: ConfigScope) {
        when (scope) {
            ConfigScope.COMMON -> commonValues.setFrom(config)
            ConfigScope.CLIENT -> clientValues.setFrom(config.client)
        }
    }

    private fun saveLoadedSpec(scope: ConfigScope) {
        when (scope) {
            ConfigScope.COMMON -> if (commonSpec.isLoaded) commonSpec.save()
            ConfigScope.CLIENT -> if (clientSpec.isLoaded) clientSpec.save()
        }
    }

    private fun loadedSpecShouldOverrideLegacy(configDir: Path, scope: ConfigScope): Boolean =
        !legacyConfigExists(configDir) && configDir.resolve(scope.fileName).exists()

    private fun legacyConfigExists(configDir: Path): Boolean =
        configDir.resolve(legacyConfigRelativePath).exists()

    internal enum class StartupConfigSource {
        LEGACY_JSON,
        NEOFORGE_TOML,
    }

    internal fun chooseStartupConfigSource(
        legacyJsonExists: Boolean,
    ): StartupConfigSource =
        // NeoForge may correct and rewrite stale TOML during startup, so TOML mtime
        // is not a reliable user-edit signal. Keep the legacy JSON as the runtime source.
        if (legacyJsonExists) StartupConfigSource.LEGACY_JSON else StartupConfigSource.NEOFORGE_TOML

    internal fun chooseReloadConfigSource(
        legacyJsonExists: Boolean,
        startupConfigLoadComplete: Boolean,
    ): StartupConfigSource =
        // ConfigTracker can emit Reloading while it corrects TOML during mod loading.
        // Until FML load completion, keep JSON authoritative for existing installs.
        if (legacyJsonExists && !startupConfigLoadComplete) {
            StartupConfigSource.LEGACY_JSON
        } else {
            StartupConfigSource.NEOFORGE_TOML
        }

    internal fun markStartupConfigLoadComplete() {
        startupConfigLoadComplete = true
    }

    private enum class ConfigScope(val fileName: String) {
        COMMON(COMMON_FILE_NAME),
        CLIENT(CLIENT_FILE_NAME),
    }

    private fun configScopeForFileName(fileName: String): ConfigScope? =
        ConfigScope.entries.firstOrNull { it.fileName == fileName }

}
