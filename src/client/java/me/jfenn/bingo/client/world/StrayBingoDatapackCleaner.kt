package me.jfenn.bingo.client.world

import me.jfenn.bingo.platform.IExecutors
import net.minecraft.client.Minecraft
import org.slf4j.Logger
import kotlin.io.path.isDirectory

/**
 * Neutralizes lobby datapacks that older versions copied into every new singleplayer world.
 *
 * World creation used to copy `bingo.zip` into the CreateWorldScreen datapack temp dir on *every*
 * world creation, so plain saves ended up with `saves/<world>/datapacks/bingo.zip`. Vanilla enables
 * any datapack found in a world folder automatically (`MinecraftServer.configurePackRepository` logs
 * "Found new data pack {}, loading it automatically"), which is why those worlds started in lobby
 * mode afterwards.
 *
 * The work itself lives in [StrayBingoDatapack.neutralizeStrays]; this only decides when to run it.
 */
internal class StrayBingoDatapackCleaner(
    private val log: Logger,
    executors: IExecutors,
) {

    init {
        executors.io.submit {
            try {
                val savesDir = Minecraft.getInstance().gameDirectory.toPath().resolve("saves")
                if (!savesDir.isDirectory()) return@submit

                val neutralized = StrayBingoDatapack.neutralizeStrays(savesDir)
                for (saveDir in neutralized) {
                    log.info(
                        "[StrayBingoDatapackCleaner] Disabled the accidentally installed lobby datapack in save '{}' " +
                            "(renamed {} to {}{}). Vanilla auto-enables datapacks in a world folder, which would " +
                            "have started that world in lobby mode. Rename it back if lobby mode was intended there.",
                        saveDir.fileName,
                        StrayBingoDatapack.DATAPACK_FILE,
                        StrayBingoDatapack.DATAPACK_FILE,
                        StrayBingoDatapack.DISABLED_SUFFIX,
                    )
                }
            } catch (e: Throwable) {
                log.warn("[StrayBingoDatapackCleaner] Failed to scan saves for stray lobby datapacks", e)
            }
        }
    }
}
