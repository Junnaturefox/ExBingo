package me.jfenn.bingo.client.world

import me.jfenn.bingo.common.BINGO_WORLD_PREFIX
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StrayBingoDatapackTest {

    private val tempDirs = mutableListOf<Path>()

    @AfterTest
    fun cleanup() {
        for (dir in tempDirs) {
            Files.walk(dir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private fun createSavesDir(): Path {
        val dir = Files.createTempDirectory(Path.of("build/tmp"), "stray-datapack-")
        tempDirs.add(dir)
        return dir.resolve("saves").also { Files.createDirectories(it) }
    }

    private fun createSave(savesDir: Path, name: String, withDatapack: Boolean): Path {
        val save = savesDir.resolve(name)
        Files.createDirectories(save.resolve("datapacks"))
        if (withDatapack) {
            Files.writeString(save.resolve("datapacks").resolve(StrayBingoDatapack.DATAPACK_FILE), "zip")
        }
        return save
    }

    @Test
    fun `the datapack is neutralized in plain saves`() {
        val savesDir = createSavesDir()
        val plain = createSave(savesDir, "世界", withDatapack = true)

        val neutralized = StrayBingoDatapack.neutralizeStrays(savesDir)

        assertEquals(listOf(plain.name), neutralized.map { it.fileName.toString() })
        assertFalse(plain.resolve("datapacks/bingo.zip").exists(), "the pack must leave the load path")
        assertTrue(plain.resolve("datapacks/bingo.zip.disabled").exists(), "nothing may be deleted")
    }

    @Test
    fun `the datapack is kept in worlds the mod created`() {
        val savesDir = createSavesDir()
        val bingoWorld = createSave(savesDir, "$BINGO_WORLD_PREFIX 世界", withDatapack = true)

        val neutralized = StrayBingoDatapack.neutralizeStrays(savesDir)

        assertTrue(neutralized.isEmpty())
        assertTrue(bingoWorld.resolve("datapacks/bingo.zip").exists())
    }

    @Test
    fun `saves without the datapack are ignored`() {
        val savesDir = createSavesDir()
        createSave(savesDir, "另一个世界", withDatapack = false)

        assertTrue(StrayBingoDatapack.neutralizeStrays(savesDir).isEmpty())
    }

    @Test
    fun `a missing saves directory is not an error`() {
        val savesDir = createSavesDir().resolve("does-not-exist")

        assertTrue(StrayBingoDatapack.neutralizeStrays(savesDir).isEmpty())
    }
}
