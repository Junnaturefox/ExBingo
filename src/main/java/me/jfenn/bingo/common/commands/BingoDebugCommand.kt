package me.jfenn.bingo.common.commands

import kotlinx.serialization.encodeToString
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.platform.IJsonSerializers
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import org.slf4j.Logger

class BingoDebugCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
    private val serializers: IJsonSerializers,
    private val log: Logger,
) : BingoComponent() {

    init {
        commandManager.register("bingo") {
            literal("debug") {
                requires {
                    hasPermission(Permission.COMMAND_DEBUG)
                }
                executes {
                    val state = scope.get<BingoState>()

                    // This must use IJsonSerializers (which registers the contextual IText and
                    // IItemStack serializers), not the plain utils.json: BingoState contains
                    // IText-typed fields such as BingoTeam.name, and encoding those without the
                    // module fails with "Serializer for subclass 'TextImpl' is not found".
                    val stateJson = try {
                        serializers.json.encodeToString(state)
                    } catch (e: Throwable) {
                        log.error("[BingoDebugCommand] Unable to serialize the game state:", e)
                        null
                    }

                    if (stateJson != null) {
                        log.info(stateJson)
                        sendMessage(text.literal("Debug info printed to server logs.").formatted(ChatFormatting.GRAY))
                    } else {
                        sendMessage(
                            text.literal("Unable to serialize the game state - see the server log for details.")
                                .formatted(ChatFormatting.RED)
                        )
                    }
                }
            }
        }
    }
}
