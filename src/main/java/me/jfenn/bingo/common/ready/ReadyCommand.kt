package me.jfenn.bingo.common.ready

import me.jfenn.bingo.common.commands.canConfigureGame
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.commands.ICommandManager

class ReadyCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) {
    init {
        commandManager.register("ready") {
            requires {
                // null-safe: console/command blocks used to throw here, which turned into
                // "unknown command" plus an ERROR log entry instead of a normal rejection
                val player = player
                player != null && scope.get<ReadyService>().isReadyEnabled(player)
            }
            executes {
                when (
                    val result = scope.get<ReadyService>().toggleReady(playerOrThrow)
                ) {
                    is ReadyResult.ChangedTo -> sendFeedback(result.message)
                    else -> error(result.message)
                }
            }

            literal("cancel") {
                requires { canConfigureGame() }
                executes {
                    // cancel() resets the timer *and* keeps the cancelled flag set; emitting
                    // OptionsChangedEvent here used to re-arm the timer immediately
                    scope.get<ReadyService>().cancel()
                    sendFeedback(text.string(StringKey.CommandReadyCancelled))
                }
            }
        }
    }
}
