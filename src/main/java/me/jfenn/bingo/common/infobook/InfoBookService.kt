package me.jfenn.bingo.common.infobook

import me.jfenn.bingo.platform.text.HoverAction
import me.jfenn.bingo.platform.text.ITextFactory
import me.jfenn.bingo.platform.text.TextAction
import me.jfenn.bingo.common.*
import me.jfenn.bingo.common.commands.BingoPrefsCommand
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.BINGO_VERSION
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.permissions.IPermissionsApi
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.item.IWrittenBook
import me.jfenn.bingo.platform.scoreboard.IScoreboardManager
import me.jfenn.bingo.platform.text.IText
import net.minecraft.ChatFormatting

internal class InfoBookService(
    private val config: BingoConfig,
    private val state: BingoState,
    private val text: TextProvider,
    private val permissions: IPermissionsApi,
    private val cardViewService: CardViewService,
    private val scoreboardManager: IScoreboardManager,
    private val playerSettingsService: PlayerSettingsService,
    private val itemStackFactory: IItemStackFactory,
    private val textFactory: ITextFactory,
) {

    private fun commandLink(label: String, suggestion: String = label) =
        textFactory.literal(label)
            .formatted(ChatFormatting.DARK_GREEN, ChatFormatting.UNDERLINE)
            .also { it.setClickEvent(TextAction.SuggestCommand(suggestion)) }

    private fun blankLine() = textFactory.literal("")

    private fun getIntroPage(player: IPlayerHandle): List<IText> = buildList {
        add(
            textFactory.empty()
                .append(
                    text.string(
                        StringKey.IntroWelcome,
                        text.string(StringKey.IntroWelcomeLink).formatted(ChatFormatting.DARK_GREEN, ChatFormatting.UNDERLINE).also {
                            it.setClickEvent(TextAction.OpenUrl(URL_EXBINGO))
                        },
                    ).formatted(ChatFormatting.BOLD)
                )
        )
        add(textFactory.literal("v$BINGO_VERSION").formatted(ChatFormatting.GRAY))
        add(blankLine())
        add(
            text.string(
                StringKey.IntroPortNote,
                text.string(StringKey.IntroPortNoteLink).formatted(ChatFormatting.DARK_GREEN, ChatFormatting.UNDERLINE).also {
                    it.setClickEvent(TextAction.OpenUrl(URL_WIKI))
                },
            ).formatted(ChatFormatting.GRAY)
        )

        if (canConfigureGame(player)) {
            add(blankLine())
            add(
                when {
                    state.isLobbyMode -> text.string(StringKey.IntroUseConfigMenu)
                    else -> text.string(
                        StringKey.IntroUseConfigCommands,
                        commandLink("/bingo", "/bingo "),
                    )
                }
            )
        }

        add(blankLine())
        add(
            text.string(
                StringKey.IntroReadHowToPlay,
                text.string(StringKey.IntroReadHowToPlayLink).formatted(ChatFormatting.DARK_GREEN, ChatFormatting.UNDERLINE).also {
                    it.setClickEvent(TextAction.OpenUrl(URL_WIKI_WHAT_IS_BINGO))
                },
            )
        )

        if (!canConfigureGame(player)) {
            add(blankLine())
            add(text.string(StringKey.IntroChangePlayerSettings))
        }

        add(blankLine())
        add(text.string(StringKey.IntroHaveFun))
    }

    private fun getHintsPage(player: IPlayerHandle): List<IText> = buildList {
        add(text.string(StringKey.IntroHints).formatted(ChatFormatting.BOLD))
        add(blankLine())
        add(
            when {
                cardViewService.supportsCardHud(player) -> text.string(
                    StringKey.IntroKeybindHud,
                    textFactory.keybind(KEYBIND_OPEN_CARD).formatted(ChatFormatting.DARK_GREEN),
                )
                else -> text.string(
                    StringKey.IntroKeybindMap,
                    textFactory.keybind("key.use").formatted(ChatFormatting.DARK_GREEN),
                )
            }
        )

        if (permissions.hasPermission(player, Permission.COMMAND_COORDS)) {
            add(blankLine())
            add(text.string(StringKey.IntroUseCoords, commandLink("/coords")))
        }
    }

    private fun getModCompatPage(): List<IText> = buildList {
        add(text.string(StringKey.IntroAutotierTitle).formatted(ChatFormatting.BOLD))
        add(blankLine())
        add(text.string(StringKey.IntroAutotierIntro))
        add(blankLine())
        add(text.string(StringKey.IntroAutotierCommand, commandLink("/bingo autotier generate")))
        add(blankLine())
        add(text.string(StringKey.IntroAutotierNote).formatted(ChatFormatting.GRAY))
    }

    private fun getCardDisablePage(): List<IText> = buildList {
        add(text.string(StringKey.IntroCarddisableTitle).formatted(ChatFormatting.BOLD))
        add(blankLine())
        add(text.string(StringKey.IntroCarddisableIntro))
        add(blankLine())
        add(
            text.string(
                StringKey.IntroCarddisable,
                commandLink("/bingo carddisable <board>", "/bingo carddisable "),
                commandLink("/bingo cardenable <board>", "/bingo cardenable "),
            )
        )
        add(blankLine())
        add(text.string(StringKey.IntroCarddisableNote).formatted(ChatFormatting.GRAY))
    }

    private fun getTeamFeaturesPage(player: IPlayerHandle): List<IText> = buildList {
        add(text.string(StringKey.IntroTeamFeaturesTitle).formatted(ChatFormatting.BOLD))
        add(blankLine())
        add(
            text.string(
                StringKey.IntroTeamFeaturesChest,
                commandLink("/teamchest"),
                commandLink("/tc"),
            )
        )
        add(
            text.string(
                StringKey.IntroTeamFeaturesChestKey,
                textFactory.keybind(KEYBIND_OPEN_TEAM_CHEST).formatted(ChatFormatting.DARK_GREEN),
            )
        )
        add(blankLine())
        add(
            text.string(
                StringKey.IntroTeamFeaturesTeleport,
                commandLink("/teamtp <player>", "/teamtp "),
                commandLink("/ttp <player>", "/ttp "),
            )
        )

        if (canConfigureGame(player)) {
            add(blankLine())
            add(
                text.string(
                    StringKey.IntroTeamFeaturesOps,
                    commandLink("/teamchest toggle"),
                    commandLink("/teamchest count"),
                    commandLink("/tptoggle"),
                ).formatted(ChatFormatting.GRAY)
            )
        }
    }

    private fun canConfigureGame(player: IPlayerHandle): Boolean =
        config.allowNonOpGameConfiguration ||
            permissions.hasPermission(player, Permission.CONFIGURE_GAME)

    private fun toggleText(
        value: Boolean,
        name: StringKey,
        description: StringKey,
    ) =
        textFactory.empty()
            .append(
                textFactory.literal(if (value) "\u2611" else "\u2610")
                    .formatted(
                        if (value) ChatFormatting.DARK_GREEN else ChatFormatting.DARK_RED,
                    )
            )
            .append(" ")
            .append(text.string(name))
            .append(
                textFactory.literal(" \uD83D\uDEC8").formatted(ChatFormatting.GRAY).also {
                    it.setHoverEvent(HoverAction.ShowText(text.string(description)))
                }
            )

    private fun toggleLine(
        value: Boolean,
        name: StringKey,
        description: StringKey,
        field: kotlin.reflect.KProperty1<PlayerSettings, Boolean>,
    ) =
        toggleText(value, name, description)
            .also { line ->
                line.setClickEvent(TextAction.RunCommand(BingoPrefsCommand.getCommand(field, !value)))
            }

    private fun getSettingsPage(player: IPlayerHandle, settings: PlayerSettings): List<IText> = buildList {
        add(text.string(StringKey.PlayerSettings).formatted(ChatFormatting.BOLD))
        add(blankLine())
        add(
            toggleLine(settings.bossbar, StringKey.PlayerSettingsBossbar, StringKey.PlayerSettingsBossbarDescription, PlayerSettings::bossbar)
        )
        add(blankLine())
        add(
            toggleLine(settings.scoreboard, StringKey.PlayerSettingsScoreboard, StringKey.PlayerSettingsScoreboardDescription, PlayerSettings::scoreboard)
        )

        if (!cardViewService.supportsCardHud(player)) {
            add(
                toggleLine(settings.scoreboardAutoHide, StringKey.PlayerSettingsScoreboardAutoHide, StringKey.PlayerSettingsScoreboardAutoHideDescription, PlayerSettings::scoreboardAutoHide)
            )
        }

        add(blankLine())
        add(textFactory.empty().append(text.string(StringKey.PlayerSettingsMessages)).append(":"))
        add(
            toggleLine(settings.leadingMessages, StringKey.PlayerSettingsMessagesLeading, StringKey.PlayerSettingsMessagesLeadingDescription, PlayerSettings::leadingMessages)
        )
        add(
            toggleLine(settings.scoreMessages, StringKey.PlayerSettingsMessagesLines, StringKey.PlayerSettingsMessagesLinesDescription, PlayerSettings::scoreMessages)
        )
        add(
            toggleLine(settings.itemMessages, StringKey.PlayerSettingsMessagesItems, StringKey.PlayerSettingsMessagesItemsDescription, PlayerSettings::itemMessages)
        )
        add(blankLine())
        add(
            toggleLine(settings.nightVision, StringKey.PlayerSettingsNightVision, StringKey.PlayerSettingsNightVisionDescription, PlayerSettings::nightVision)
        )
    }

    private fun isBookItem(stack: IWrittenBook) =
        stack.hasCustomTag(NBT_BINGO_INFO_BOOK)

    private fun createBookItem(player: IPlayerHandle) : IWrittenBook {
        val stack = itemStackFactory.createWrittenBook()
        stack.addCustomTag(NBT_BINGO_INFO_BOOK)
        stack.addCustomTag(NBT_BINGO_IGNORE)
        stack.addCustomTag(NBT_BINGO_VANISH)

        if (state.isLobbyMode && state.state == GameState.PREGAME && config.lobbyTutorialBook)
            stack.addCustomTag(NBT_BINGO_KEEP)

        stack.title = text.raw(StringKey.FullName)
        stack.author = player.playerName
        updateBookItem(player, stack)
        return stack
    }

    /**
     * Builds the book pages.
     *
     * Each section is a list of single-line components; [BookPaginator] then splits every section
     * into as many book pages as it needs, because a vanilla page only renders 14 wrapped lines
     * and silently drops the rest.
     */
    private fun buildSections(player: IPlayerHandle, settings: PlayerSettings): List<List<IText>> = buildList {
        if (settings.seenTutorial) {
            if (permissions.hasPermission(player, Permission.CONFIGURE_PLAYER))
                add(getSettingsPage(player, settings))
            add(getIntroPage(player))
            add(getHintsPage(player))
        } else {
            add(getIntroPage(player))
            if (permissions.hasPermission(player, Permission.CONFIGURE_PLAYER))
                add(getSettingsPage(player, settings))
            add(getHintsPage(player))
        }

        add(getTeamFeaturesPage(player))

        // mod compatibility & auto-tiering (ops only — it can write server config)
        if (permissions.hasPermission(player, Permission.COMMAND_AUTOTIER))
            add(getModCompatPage())

        // disabling boards (ops only — it writes server config)
        if (permissions.hasPermission(player, Permission.COMMAND_CARD_TOGGLE))
            add(getCardDisablePage())
    }

    private fun updateBookItem(player: IPlayerHandle, stack: IWrittenBook) {
        val settings = playerSettingsService.getPlayer(player)
        stack.setPages(BookPaginator.paginate(buildSections(player, settings), textFactory))
    }

    fun updateBookItem(player: IPlayerHandle) {
        player.allHeldStacks()
            .mapNotNull { it.asWrittenBook() }
            .filter { isBookItem(it) }
            .forEach { updateBookItem(player, it) }
    }

    fun giveBookItem(player: IPlayerHandle) {
        var bookCount = 0
        for (view in player.allHeldStackViews()) {
            val stack = view.stack.asWrittenBook() ?: continue
            if (isBookItem(stack)) {
                // prevent players from taking someone else's book...
                if (stack.author != scoreboardManager.getPlayerName(player))
                    updateBookItem(player, stack)

                bookCount++

                if (bookCount > 1)
                    view.mutate { it.count = 0 }
                else if (stack.count > 1)
                    view.mutate { it.count = 1 }
            }
        }

        if (bookCount == 0) {
            val bookItem = createBookItem(player)
            player.giveItemStack(bookItem)
        }
    }
}
