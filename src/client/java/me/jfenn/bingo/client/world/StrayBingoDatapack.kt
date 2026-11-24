package me.jfenn.bingo.client.world

import me.jfenn.bingo.common.BINGO_WORLD_PREFIX
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Helpers for the lobby datapack that the mod puts into a save folder.
 *
 * Vanilla automatically enables every datapack it discovers in `world/datapacks/`
 * (`MinecraftServer.configurePackRepository` logs "Found new data pack {}, loading it
 * automatically"), so a stray `bingo.zip` in a plain save makes that save a lobby world.
 */
internal object StrayBingoDatapack {

    const val DATAPACK_FILE = "bingo.zip"

    /** Suffix used to neutralize a datapack without deleting it (`*.zip` is the only thing loaded). */
    const val DISABLED_SUFFIX = ".disabled"

    /**
     * Whether a singleplayer save is one that the mod created for a lobby.
     *
     * The mod's own bingo worlds are always named with [BINGO_WORLD_PREFIX], so they are the only
     * saves that are supposed to contain the pack. Every other save that still has it got it from
     * the world-creation bug and would silently start in lobby mode.
     */
    fun isModCreatedLobbyWorld(saveName: String): Boolean = saveName.startsWith(BINGO_WORLD_PREFIX)

    /**
     * Renames a stray [DATAPACK_FILE] in every save under [savesDir] to `bingo.zip.disabled`,
     * leaving the mod's own lobby worlds untouched. Returns the saves that were changed.
     *
     * The file is renamed rather than deleted so that nothing is lost if a player really did want
     * lobby mode in that save; `*.zip.disabled` is ignored by the datapack loader.
     */
    fun neutralizeStrays(savesDir: Path): List<Path> {
        if (!savesDir.isDirectory()) return emptyList()

        val neutralized = mutableListOf<Path>()
        Files.list(savesDir).use { streaming ->
            for (saveDir in streaming.filter { it.isDirectory() }.toList()) {
                if (isModCreatedLobbyWorld(saveDir.fileName.toString())) continue

                val datapack = saveDir.resolve("datapacks").resolve(DATAPACK_FILE)
                if (!datapack.exists()) continue

                Files.move(
                    datapack,
                    datapack.resolveSibling(DATAPACK_FILE + DISABLED_SUFFIX),
                    StandardCopyOption.REPLACE_EXISTING,
                )
                neutralized.add(saveDir)
            }
        }
        return neutralized
    }
}
