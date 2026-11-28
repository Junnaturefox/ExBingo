package me.jfenn.bingo.common.config

import kotlinx.serialization.encodeToString
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.autotier.AutoTierConfig
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.utils.json
import net.neoforged.neoforge.common.ModConfigSpec
import java.util.function.Predicate
import java.util.UUID
import java.util.function.Supplier


/** Value used by the config screen for "disable this feature". */
internal const val DISABLED_SECONDS = -1

data class ConfigEntry(
    val configPath: String,
    val translationKey: String,
)

data class ConfigSection(
    val translationKey: String,
)

internal fun commonSectionKey(section: String) = "$MOD_ID_BINGO.configuration.common.$section"
internal fun clientSectionKey(section: String) = "$MOD_ID_BINGO.configuration.client.$section"
internal fun commonKey(section: String, name: String) = "${commonSectionKey(section)}.$name"
internal fun clientKey(section: String, name: String) = "${clientSectionKey(section)}.$name"

internal fun ModConfigSpec.Builder.pushSection(
    path: String,
    comment: String,
    translationKey: String,
    sections: MutableList<ConfigSection>,
) {
    sections += ConfigSection(translationKey)
    comment(comment).translation(translationKey).push(path)
}

internal fun ModConfigSpec.Builder.value(
    configPath: String,
    translationKey: String,
    comment: String,
    entries: MutableList<ConfigEntry>,
): ModConfigSpec.Builder {
    entries += ConfigEntry(configPath, translationKey)
    return comment(comment).translation(translationKey)
}

internal fun ModConfigSpec.Builder.defineBoolean(
    configPath: String,
    translationKey: String,
    comment: String,
    tomlPath: String,
    defaultValue: () -> Boolean,
    entries: MutableList<ConfigEntry>,
): ModConfigSpec.BooleanValue =
    value(configPath, translationKey, comment, entries)
        .define(tomlPathSegments(tomlPath), Supplier { defaultValue() })

internal fun ModConfigSpec.Builder.defineInt(
    configPath: String,
    translationKey: String,
    comment: String,
    tomlPath: String,
    defaultValue: () -> Int,
    entries: MutableList<ConfigEntry>,
    min: Int,
    max: Int,
): ModConfigSpec.ConfigValue<Int> =
    value(configPath, translationKey, "$comment Range: $min ~ $max.", entries)
        .define(tomlPathSegments(tomlPath), Supplier { defaultValue() }, Predicate { it is Number && it.toInt() in min..max }, Int::class.java)

internal fun ModConfigSpec.Builder.defineDouble(
    configPath: String,
    translationKey: String,
    comment: String,
    tomlPath: String,
    defaultValue: () -> Double,
    entries: MutableList<ConfigEntry>,
    min: Double,
    max: Double,
): ModConfigSpec.ConfigValue<Double> =
    value(configPath, translationKey, "$comment Range: $min ~ $max.", entries)
        .define(tomlPathSegments(tomlPath), Supplier { defaultValue() }, Predicate { it is Number && it.toDouble() in min..max }, Double::class.java)

internal fun ModConfigSpec.Builder.defineString(
    configPath: String,
    translationKey: String,
    comment: String,
    tomlPath: String,
    defaultValue: () -> String,
    entries: MutableList<ConfigEntry>,
    allowBlank: Boolean,
    validator: (String) -> Boolean = { true },
): ModConfigSpec.ConfigValue<String> =
    value(configPath, translationKey, comment, entries)
        .define(
            tomlPathSegments(tomlPath),
            Supplier { defaultValue() },
            Predicate { it is String && (allowBlank || it.isNotBlank()) && validator(it) },
            String::class.java,
        )

internal inline fun <reified T> ModConfigSpec.Builder.defineEnum(
    configPath: String,
    translationKey: String,
    comment: String,
    tomlPath: String,
    noinline defaultValue: () -> T,
    entries: MutableList<ConfigEntry>,
): ModConfigSpec.EnumValue<T> where T : Enum<T> =
    value(configPath, translationKey, comment, entries)
        .defineEnum(
            tomlPathSegments(tomlPath),
            Supplier { defaultValue() },
            Predicate { value ->
                value is T || (value is String && enumValues<T>().any { it.name.equals(value, ignoreCase = true) })
            },
            T::class.java,
        )

internal fun ModConfigSpec.Builder.defineStringList(
    configPath: String,
    translationKey: String,
    comment: String,
    tomlPath: String,
    defaultValue: () -> List<String>,
    entries: MutableList<ConfigEntry>,
    allowBlank: Boolean,
    validator: (String) -> Boolean = { true },
): ModConfigSpec.ConfigValue<List<String>> =
    value(configPath, translationKey, comment, entries)
        .defineList(
            tomlPathSegments(tomlPath),
            Supplier<List<String>> { defaultValue() },
            Supplier { "" },
            Predicate { it is String && (allowBlank || it.isNotBlank()) && validator(it) },
        ) as ModConfigSpec.ConfigValue<List<String>>

internal fun ModConfigSpec.Builder.defineDoubleList(
    configPath: String,
    translationKey: String,
    comment: String,
    tomlPath: String,
    defaultValue: () -> List<Double>,
    entries: MutableList<ConfigEntry>,
    min: Double,
    max: Double,
): ModConfigSpec.ConfigValue<List<Double>> =
    value(configPath, translationKey, "$comment Each value must be in range $min ~ $max.", entries)
        .defineList(
            tomlPathSegments(tomlPath),
            Supplier<List<Double>> { defaultValue() },
            Supplier { 0.0 },
            Predicate { it is Number && it.toDouble() in min..max },
        ) as ModConfigSpec.ConfigValue<List<Double>>

internal fun tomlPathSegments(tomlPath: String): List<String> =
    tomlPath.split('.')

internal fun String.blankToNull(): String? = takeIf { it.isNotBlank() }

internal fun itemFilterPresetList(presets: Map<String, ObjectiveFilterList>): List<String> =
    presets.entries
        .sortedBy { it.key }
        .map { (name, filter) -> "$name=$filter" }

internal fun parseItemFilterPresetList(values: List<String>): Map<String, ObjectiveFilterList> =
    values.mapNotNull { value ->
        val name = value.substringBefore('=').trim()
        val filter = value.substringAfter('=', "").trim()
        if (name.isNotBlank()) {
            name to ObjectiveFilterList.fromString(filter)
        } else {
            null
        }
    }.toMap(LinkedHashMap()).ifEmpty { ObjectiveFilterList.PRESETS }

internal fun mergeItemFilterPresets(
    configPresets: Map<String, ObjectiveFilterList>,
    tomlValues: List<String>,
): Map<String, ObjectiveFilterList> {
    val tomlPresets = parseItemFilterPresetList(tomlValues)
    return (configPresets + tomlPresets)
        .takeIf { it.isNotEmpty() }
        ?: ObjectiveFilterList.PRESETS
}

internal fun isItemFilterPresetEntry(value: String): Boolean =
    value.substringBefore('=').trim().isNotBlank() && value.contains('=')

internal fun difficultyPresetList(presets: Map<String, List<Int>>): List<String> =
    presets.entries
        .map { (name, distribution) -> "$name=${distribution.joinToString(",")}" }

internal fun parseDifficultyPresetList(values: List<String>): Map<String, List<Int>> =
    values.mapNotNull { value ->
        val name = value.substringBefore('=').trim()
        val distribution = parseDifficultyDistribution(value.substringAfter('=', ""))
        if (name.isNotBlank() && distribution != null) {
            name to distribution
        } else {
            null
        }
    }.toMap(LinkedHashMap()).ifEmpty { TierLabel.DIFFICULTY_PRESETS }

internal fun isDifficultyPresetEntry(value: String): Boolean {
    val name = value.substringBefore('=').trim()
    val distribution = parseDifficultyDistribution(value.substringAfter('=', ""))
    return name.isNotBlank() && distribution != null
}

internal fun parseDifficultyDistribution(value: String): List<Int>? {
    val distribution = value.split(',')
        .map { it.trim().toIntOrNull() ?: return null }
    return distribution.takeIf {
        it.size == TierLabel.entries.size &&
                it.all { count -> count >= 0 } &&
                it.sum() == 25
    }
}

internal fun boardSourceWeightList(weights: Map<String, Double>): List<String> =
    weights.entries
        .sortedBy { it.key }
        .map { (source, weight) -> "$source=$weight" }

internal fun parseBoardSourceWeightList(values: List<String>): Map<String, Double> =
    values.mapNotNull { value ->
        val source = value.substringBefore('=').trim()
        val weight = value.substringAfter('=', "").trim().toDoubleOrNull()
        if (source.isNotBlank() && weight != null && weight.isFinite() && weight >= 0.0) {
            source to weight
        } else {
            null
        }
    }.toMap(LinkedHashMap())

internal fun isBoardSourceWeightEntry(value: String): Boolean {
    val source = value.substringBefore('=').trim()
    val weight = value.substringAfter('=', "").trim().toDoubleOrNull()
    return source.isNotBlank() && weight != null && weight.isFinite() && weight >= 0.0
}

internal fun soundVolumeList(soundVolumes: Map<String, Float>): List<String> =
    soundVolumes.entries
        .sortedBy { it.key }
        .map { (sound, volume) -> "$sound=$volume" }

internal fun parseSoundVolumeList(values: List<String>): MutableMap<String, Float> =
    values.mapNotNull { value ->
        val sound = value.substringBefore('=').trim()
        val volume = value.substringAfter('=', "").trim().toFloatOrNull()
        if (sound.isNotBlank() && volume != null && volume in 0f..1f) {
            sound to volume
        } else {
            null
        }
    }.toMap(LinkedHashMap()).toMutableMap()

internal fun isSoundVolumeEntry(value: String): Boolean {
    val sound = value.substringBefore('=').trim()
    val volume = value.substringAfter('=', "").trim().toFloatOrNull()
    return sound.isNotBlank() && volume != null && volume in 0f..1f
}

/** The default configuration used to derive TOML defaults. */
internal fun defaultConfig(): BingoConfig = BingoConfig()
