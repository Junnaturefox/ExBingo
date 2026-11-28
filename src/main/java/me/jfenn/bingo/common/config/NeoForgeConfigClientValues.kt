package me.jfenn.bingo.common.config

import net.neoforged.neoforge.common.ModConfigSpec

class ClientValues(builder: ModConfigSpec.Builder) {
    val entries = mutableListOf<ConfigEntry>()
    val sections = mutableListOf<ConfigSection>()

    init {
        builder.pushSection("hud", "Client-side Bingo HUD display settings.", clientSectionKey("hud"), sections)
    }

    val enableHud = builder.defineBoolean(
        "client.enableHud",
        clientKey("hud", "enable_hud"),
        "Renders the client-side Bingo card HUD.",
        "enableHud",
        { defaultConfig().client.enableHud },
        entries,
    )
    val showQuickStartButton = builder.defineBoolean(
        "client.showQuickStartButton",
        clientKey("hud", "show_quick_start_button"),
        "Adds a Bingo button to the title screen for quick single-player setup.",
        "showQuickStartButton",
        { defaultConfig().client.showQuickStartButton },
        entries,
    )
    val cardPausesGame = builder.defineBoolean(
        "client.cardPausesGame",
        clientKey("hud", "card_pauses_game"),
        "Pauses single-player while the Bingo card screen is open.",
        "cardPausesGame",
        { defaultConfig().client.cardPausesGame },
        entries,
    )
    val cardScale = builder.defineDouble(
        "client.cardScale",
        clientKey("hud", "card_scale"),
        "Bingo card HUD scale relative to other GUI elements.",
        "cardScale",
        { defaultConfig().client.cardScale.toDouble() },
        entries,
        min = 0.25,
        max = 4.0,
    )
    val cardAlignment = builder.defineEnum(
        "client.cardAlignment",
        clientKey("hud", "card_alignment"),
        "Screen corner used to anchor the Bingo card HUD.",
        "cardAlignment",
        { defaultConfig().client.cardAlignment },
        entries,
    )
    val cardOffsetX = builder.defineInt(
        "client.cardOffsetX",
        clientKey("hud", "card_offset_x"),
        "Horizontal distance between the Bingo card and its anchored screen edge.",
        "cardOffsetX",
        { defaultConfig().client.cardOffsetX },
        entries,
        min = -10000,
        max = 10000,
    )
    val cardOffsetY = builder.defineInt(
        "client.cardOffsetY",
        clientKey("hud", "card_offset_y"),
        "Vertical distance between the Bingo card and its anchored screen edge.",
        "cardOffsetY",
        { defaultConfig().client.cardOffsetY },
        entries,
        min = -10000,
        max = 10000,
    )
    val cardOverlap = builder.defineEnum(
        "client.cardOverlap",
        clientKey("hud", "card_overlap"),
        "Controls whether the card draws above or underneath other HUD elements.",
        "cardOverlap",
        { defaultConfig().client.cardOverlap },
        entries,
    )
    val cardTeamOutlines = builder.defineBoolean(
        "client.cardTeamOutlines",
        clientKey("hud", "card_team_outlines"),
        "Draws outlines when other teams have captured a card tile.",
        "cardTeamOutlines",
        { defaultConfig().client.cardTeamOutlines },
        entries,
    )
    val showMultipleCards = builder.defineBoolean(
        "client.showMultipleCards",
        clientKey("hud", "show_multiple_cards"),
        "Attempts to draw all available cards in the HUD when space allows.",
        "showMultipleCards",
        { defaultConfig().client.showMultipleCards },
        entries,
    )
    val showItemDifficulties = builder.defineBoolean(
        "client.showItemDifficulties",
        clientKey("hud", "show_item_difficulties"),
        "Draws S/A/B/C/D difficulty letters on item and advancement icons that have an ExBingo tier.",
        "showItemDifficulties",
        { defaultConfig().client.showItemDifficulties },
        entries,
    )
    val hideOnF3 = builder.defineBoolean(
        "client.hideOnF3",
        clientKey("hud", "hide_on_f3"),
        "Hides the Bingo card HUD while the debug screen is open.",
        "hideOnF3",
        { defaultConfig().client.hideOnF3 },
        entries,
    )
    val hideOnChat = builder.defineBoolean(
        "client.hideOnChat",
        clientKey("hud", "hide_on_chat"),
        "Hides the Bingo card HUD while the chat screen is open.",
        "hideOnChat",
        { defaultConfig().client.hideOnChat },
        entries,
    )

    init {
        builder.pop()
        builder.pushSection("messages", "Floating score message display settings.", clientSectionKey("messages"), sections)
    }

    val messageFromOtherTeams = builder.defineBoolean(
        "client.messageFromOtherTeams",
        clientKey("messages", "message_from_other_teams"),
        "Shows item capture messages from other teams.",
        "messageFromOtherTeams",
        { defaultConfig().client.messageFromOtherTeams },
        entries,
    )
    val messageDurationSeconds = builder.defineInt(
        "client.messageDurationSeconds",
        clientKey("messages", "message_duration_seconds"),
        "Seconds each floating message remains visible.",
        "messageDurationSeconds",
        { defaultConfig().client.messageDurationSeconds },
        entries,
        min = 0,
        max = 3600,
    )
    val messageScale = builder.defineDouble(
        "client.messageScale",
        clientKey("messages", "message_scale"),
        "Floating message scale relative to other GUI elements.",
        "messageScale",
        { defaultConfig().client.messageScale.toDouble() },
        entries,
        min = 0.25,
        max = 4.0,
    )

    init {
        builder.pop()
        builder.pushSection("sounds", "Per-sound client notification volumes.", clientSectionKey("sounds"), sections)
    }

    val soundVolumes = builder.defineStringList(
        "client.soundVolumes",
        clientKey("sounds", "sound_volumes"),
        "Per-sound volume overrides written as sound_id=volume.",
        "soundVolumes",
        { soundVolumeList(defaultConfig().client.soundVolumes) },
        entries,
        allowBlank = false,
        validator = ::isSoundVolumeEntry,
    )

    init {
        builder.pop()
    }

    fun applyTo(config: ClientConfig): ClientConfig =
        config.copy(
            enableHud = enableHud.get(),
            showQuickStartButton = showQuickStartButton.get(),
            cardPausesGame = cardPausesGame.get(),
            cardScale = cardScale.get().toFloat(),
            cardAlignment = cardAlignment.get(),
            cardOffsetX = cardOffsetX.get(),
            cardOffsetY = cardOffsetY.get(),
            cardOverlap = cardOverlap.get(),
            cardTeamOutlines = cardTeamOutlines.get(),
            showMultipleCards = showMultipleCards.get(),
            showItemDifficulties = showItemDifficulties.get(),
            hideOnF3 = hideOnF3.get(),
            hideOnChat = hideOnChat.get(),
            messageFromOtherTeams = messageFromOtherTeams.get(),
            messageDurationSeconds = messageDurationSeconds.get(),
            messageScale = messageScale.get().toFloat(),
            soundVolumes = parseSoundVolumeList(soundVolumes.get()),
        )

    fun setFrom(config: ClientConfig) {
        enableHud.set(config.enableHud)
        showQuickStartButton.set(config.showQuickStartButton)
        cardPausesGame.set(config.cardPausesGame)
        cardScale.set(config.cardScale.toDouble())
        cardAlignment.set(config.cardAlignment)
        cardOffsetX.set(config.cardOffsetX)
        cardOffsetY.set(config.cardOffsetY)
        cardOverlap.set(config.cardOverlap)
        cardTeamOutlines.set(config.cardTeamOutlines)
        showMultipleCards.set(config.showMultipleCards)
        showItemDifficulties.set(config.showItemDifficulties)
        hideOnF3.set(config.hideOnF3)
        hideOnChat.set(config.hideOnChat)
        messageFromOtherTeams.set(config.messageFromOtherTeams)
        messageDurationSeconds.set(config.messageDurationSeconds)
        messageScale.set(config.messageScale.toDouble())
        soundVolumes.set(soundVolumeList(config.soundVolumes))
    }
}

