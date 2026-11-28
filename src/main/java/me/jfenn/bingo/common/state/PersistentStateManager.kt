package me.jfenn.bingo.common.state

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import me.jfenn.bingo.common.event.model.StateChangedEvent
import me.jfenn.bingo.mixinhandler.ServerEventMixinHandler
import me.jfenn.bingo.platform.IJsonSerializers
import me.jfenn.bingo.platform.IPersistentStateManager
import me.jfenn.bingo.platform.IPersistentStateType
import me.jfenn.bingo.platform.IServerWorldFactory
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.ScopeStopped
import me.jfenn.bingo.platform.event.model.ApplicationCloseEvent
import me.jfenn.bingo.platform.event.model.ServerEvent
import org.slf4j.Logger
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.reflect.KType

/** On-disk format version; bump when the saved shape changes. Files without it are legacy (v1). */
private const val STATE_FORMAT_VERSION = 2
private const val STATE_FORMAT_VERSION_KEY = "formatVersion"
private const val STATE_KEY = "state"

@OptIn(ExperimentalSerializationApi::class)
class PersistentStateManager(
    val logger: Logger,
    serializers: IJsonSerializers,
    val serverWorldFactory: IServerWorldFactory,
    eventBus: IEventBus,
) : IPersistentStateManager {

    private val json = Json(serializers.json) {
        prettyPrint = true
    }
    private val registeredTypes = mutableMapOf<String, Type<*>>()
    /** Paths that must never be overwritten because their content could not be preserved. */
    private val writeBlockedPaths = java.util.Collections.synchronizedSet(mutableSetOf<Path>())
    private val writeOptions = arrayOf<OpenOption>(
        StandardOpenOption.SYNC,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    );

    override fun <T : Any> register(
        id: String,
        kType: KType,
        default: () -> T
    ): IPersistentStateType<T> {
        val path = serverWorldFactory.overworld.directory.resolve("data")
            .resolve("$id.json.gz")

        val type = Type(
            id,
            path,
            @Suppress("UNCHECKED_CAST") (serializer(kType) as KSerializer<T>),
            default()
        )
        registeredTypes[type.id] = type
        return type
    }

    override fun <T : Any> getFromWorld(
        type: IPersistentStateType<T>
    ): T {
        require(type is Type)
        val path = type.path
        val backupPath = backupPath(path)
        if (Files.exists(path)) {
            try {
                logger.debug("Reading persistent state ${type.id}...")
                type.value = readFromPath(path, type.serializer)
            } catch (e: Throwable) {
                logger.error("Error decoding persistent state ${type.id}", e)
                if (e is NewerStateFormatException) quarantineNewerFile(e)
                if (Files.exists(backupPath)) {
                    try {
                        logger.warn("Reading persistent state ${type.id} from backup...")
                        type.value = readFromPath(backupPath, type.serializer)
                    } catch (backupError: Throwable) {
                        logger.error("Error decoding backup persistent state ${type.id}", backupError)
                        if (backupError is NewerStateFormatException) quarantineNewerFile(backupError)
                    }
                }
            }
        }

        return type.value
    }

    override fun <T : Any> put(
        type: IPersistentStateType<T>,
        value: T
    ) {
        require(type is Type)
        val path = type.path
        val backupPath = backupPath(path)

        if (path in writeBlockedPaths) {
            // A newer-format file could not be moved aside; never destroy it with defaults.
            logger.error(
                "Refusing to overwrite persistent state '${path.fileName}': it was written by a newer " +
                    "build and could not be preserved. Upgrade ExBingo to read it, or delete it manually."
            )
            return
        }

        var didWrite = false

        try {
            logger.debug("Writing persistent state ${type.id}...")
            Files.createDirectories(path.parent)
            val tempPath = Files.createTempFile(path.parent, "${path.fileName}.", ".tmp")
            try {
                writeToPath(tempPath, type.serializer, value)
                readFromPath(tempPath, type.serializer)

                copyCurrentToBackup(path, backupPath, type.serializer)

                moveReplacing(tempPath, path)
                didWrite = true
            } finally {
                Files.deleteIfExists(tempPath)
            }
        } catch (e: Throwable) {
            logger.error("Error encoding persistent state ${type.id}", e)
        }

        if (didWrite) {
            type.value = value
            logger.debug("Writing persistent state ${type.id}... Done!")
        }
    }

    private class Type<T: Any>(
        val id: String,
        val path: Path,
        val serializer: KSerializer<T>,
        var value: T,
    ) : IPersistentStateType<T>

    private fun <T : Any> readFromPath(path: Path, serializer: KSerializer<T>): T {
        val text = Files.newInputStream(path)
            .buffered()
            .let { GZIPInputStream(it) }
            .use { it.readBytes().decodeToString() }

        val element = json.parseToJsonElement(text)
        val stateElement = if (element is JsonObject && element.containsKey(STATE_FORMAT_VERSION_KEY)) {
            val version = element[STATE_FORMAT_VERSION_KEY]?.jsonPrimitive?.intOrNull
                ?: throw IllegalStateException("Persistent state '${path.fileName}' has an unreadable '$STATE_FORMAT_VERSION_KEY'")
            if (version > STATE_FORMAT_VERSION) {
                // Never parse a newer format with older rules - that would silently mangle the
                // player's round. The caller quarantines the file and falls back to the backup.
                throw NewerStateFormatException(path, version)
            }
            element[STATE_KEY]
                ?: throw IllegalStateException("Persistent state '${path.fileName}' is missing '$STATE_KEY'")
        } else {
            // Files written before the envelope existed are a bare serialized value; they are still
            // valid and get rewritten in the current format on the next save.
            element
        }

        return json.decodeFromJsonElement(serializer, stateElement)
    }

    private fun <T : Any> writeToPath(path: Path, serializer: KSerializer<T>, value: T) {
        val envelope = buildJsonObject {
            put(STATE_FORMAT_VERSION_KEY, JsonPrimitive(STATE_FORMAT_VERSION))
            put(STATE_KEY, json.encodeToJsonElement(serializer, value))
        }
        val bytes = json.encodeToString(JsonElement.serializer(), envelope).encodeToByteArray()

        Files.newOutputStream(path, *writeOptions)
            .buffered()
            .let { GZIPOutputStream(it) }
            .use { it.write(bytes) }
    }

    private fun backupPath(path: Path): Path {
        return path.resolveSibling("${path.fileName}.bak")
    }

    /**
     * Preserve a state file that was written by a newer build instead of letting the next save
     * overwrite it with defaults. The original is renamed to `*.newer`; if it cannot be moved the
     * path is blocked so [put] refuses to write over it.
     */
    private fun quarantineNewerFile(error: NewerStateFormatException) {
        val file = error.file
        val target = file.resolveSibling("${file.fileName}.newer")
        try {
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING)
            logger.error(
                "Persistent state '${file.fileName}' was written by a newer ExBingo build " +
                    "(format ${error.version}, supported up to $STATE_FORMAT_VERSION). Its data was " +
                    "preserved as '${target.fileName}'; defaults are used until the mod is upgraded."
            )
        } catch (moveError: Throwable) {
            writeBlockedPaths.add(file)
            logger.error(
                "Persistent state '${file.fileName}' is from a newer build and could not be moved " +
                    "aside; refusing to overwrite it. Upgrade ExBingo, or delete it to start fresh.",
                moveError,
            )
        }
    }

    private fun <T : Any> copyCurrentToBackup(path: Path, backupPath: Path, serializer: KSerializer<T>) {
        if (!Files.exists(path)) return

        val backupTempPath = Files.createTempFile(path.parent, "${backupPath.fileName}.", ".tmp")
        try {
            try {
                readFromPath(path, serializer)
            } catch (e: Throwable) {
                logger.warn("Skipping backup update because current persistent state is unreadable: {}", path, e)
                return
            }

            Files.copy(path, backupTempPath, StandardCopyOption.REPLACE_EXISTING)
            readFromPath(backupTempPath, serializer)
            moveReplacing(backupTempPath, backupPath)
        } finally {
            Files.deleteIfExists(backupTempPath)
        }
    }

    private fun moveReplacing(from: Path, to: Path) {
        try {
            Files.move(
                from,
                to,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: UnsupportedOperationException) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun writeAll() {
        registeredTypes.values.forEach {
            this.put<Any>(@Suppress("UNCHECKED_CAST") (it as Type<Any>), it.value)
        }
    }

    init {
        // Bind the vanilla save hook to this scope's event bus, so autosave / /save-all writes the
        // bingo state out with the world. Without this the hook is inert and progress is only
        // flushed on state changes and shutdown.
        //
        // The server is resolved defensively: unit tests construct this class with a fake world
        // factory whose `world` is not a real ServerLevel. In a running server the lookup always
        // succeeds (the scope only exists while the server does).
        val hookServer = runCatching { serverWorldFactory.overworld.world.server }.getOrNull()
        if (hookServer != null) {
            ServerEventMixinHandler.register(hookServer, eventBus)
        } else {
            logger.debug("[PersistentStateManager] No server available; the save hook stays unbound.")
        }

        eventBus.register(ServerEvent.Saved) {
            writeAll()
        }

        eventBus.register(StateChangedEvent) {
            writeAll()
        }

        eventBus.register(ScopeStopped) {
            if (hookServer != null) ServerEventMixinHandler.unregister(hookServer)
            writeAll()
        }

        eventBus.register(ApplicationCloseEvent) {
            writeAll()
        }
    }
}
/** Raised when a state file was written by a newer build than this one. */
private class NewerStateFormatException(
    val file: Path,
    val version: Int,
) : IllegalStateException(
    "Persistent state '" + file.fileName + "' uses format " + version +
        " (supported up to " + STATE_FORMAT_VERSION + ")"
)