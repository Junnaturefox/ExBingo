package me.jfenn.bingo.common.chat

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.commands.ICommandManager
import net.minecraft.ChatFormatting
import org.slf4j.LoggerFactory

class ChatCommand(
    commandManager: ICommandManager,
    config: BingoConfig,
    private val text: TextProvider,
) {
    private val log = LoggerFactory.getLogger("ExBingo")

    /**
     * [getArgument] returns a future that completes off the command thread, so a failure inside the
     * handler cannot be reported by throwing - it would be swallowed by the future and the player
     * would get neither the message nor an error.
     */
    private fun handleAsync(sender: IPlayerHandle, action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            log.error("[ChatCommand] Failed to send chat message from {}", sender.playerName, e)
            sender.sendMessage(
                text.literal(e.message ?: "Unable to send that message").formatted(ChatFormatting.RED)
            )
        }
    }

    init {
        // send a message to all teams, outside of the team-local chat
        for (cmd in config.chat.globalCommandAliases) {
            commandManager.register(cmd) {
                signedMessage("message") { messageArg ->
                    executes {
                        val sender = playerOrThrow
                        getArgument(messageArg).thenAccept { message ->
                            handleAsync(sender) {
                                scope.get<ChatMessageService>().sendGlobalMessage(message, sender)
                            }
                        }
                    }
                }
            }
        }

        // send a message to the team-local chat
        for (cmd in config.chat.teamCommandAliases) {
            commandManager.register(cmd) {
                signedMessage("message") { messageArg ->
                    executes {
                        val sender = playerOrThrow
                        getArgument(messageArg).thenAccept { message ->
                            handleAsync(sender) {
                                scope.get<ChatMessageService>().sendTeamMessage(message, sender)
                            }
                        }
                    }
                }
            }
        }
    }
}
