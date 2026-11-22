package me.jfenn.bingo.integrations.voice

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.model.TeamWinnerEvent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.config.IConfigManager
import me.jfenn.bingo.platform.config.read
import me.jfenn.bingo.platform.config.write
import me.jfenn.bingo.platform.event.IEventBus

internal class VoiceGroupController(
    configManager: IConfigManager,
    private val playerManager: IPlayerManager,
    private val voiceApi: IVoiceApi,
    private val state: BingoState,
    private val teamService: TeamService,
    scopedEvents: ScopedEvents,
    eventBus: IEventBus,
) {

    companion object {
        private const val CONFIG_PATH = "exbingo/voicechat.json"
    }

    val config: VoiceConfig = try {
        configManager.read(CONFIG_PATH)
    } catch (e: Throwable) {
        VoiceConfig()
    }.also {
        if (voiceApi.isInstalled()) {
            configManager.write(CONFIG_PATH, it)
        }
    }

    private var usingCombinedGroup: Boolean? = null
    private fun isUsingCombinedGroup(): Boolean {
        usingCombinedGroup?.let { return it }

        val isSingleplayerTeams = state.getRegisteredTeams().all { it.players.size <= 1 }
        return (isSingleplayerTeams && config.useCombinedGroupForSingleplayerTeams)
            .also { usingCombinedGroup = it }
    }

    private var combinedGroup: IGroupHandle? = null
    private var spectatorsGroup: IGroupHandle? = null

    // Groups are created on demand here. The PREGAME cleanup must NOT go through these helpers:
    // creating a group is a side effect, and SimpleVoiceApi.createGroup reuses an existing group by
    // name, so cleanup would otherwise create-and-destroy a group on every return to the lobby and
    // could delete a group this mod does not own.
    private fun ensureCombinedGroup(): IGroupHandle? =
        combinedGroup ?: config.combinedGroup?.let { voiceApi.createGroup(it) }?.also { combinedGroup = it }

    private fun ensureSpectatorsGroup(): IGroupHandle? =
        spectatorsGroup ?: config.spectatorGroup?.let { voiceApi.createGroup(it) }?.also { spectatorsGroup = it }

    private val teamGroups = mutableMapOf<String, IGroupHandle?>()
    private fun createTeamGroup(team: BingoTeam): IGroupHandle? {
        return teamGroups.getOrPut(team.id) {
            val settings = config.teamGroups ?: return@getOrPut null
            val teamName = team.key.label.formatTitle()
            val name = when {
                settings.name.contains("%s") -> settings.name.replace("%s", teamName)
                else -> "${settings.name}: $teamName"
            }
            voiceApi.createGroup(settings.copy(name = name))
        }
    }

    private fun getGroup(team: BingoTeam?): IGroupHandle? {
        // voicechat.json exposes `enabled`; honour it here, the single entry point for all group
        // assignment, so an admin can actually turn the integration off.
        if (!config.enabled) return null

        return when {
            state.state == GameState.PREGAME -> null
            state.state == GameState.POSTGAME -> ensureCombinedGroup()
            config.useCombinedGroupAlways -> ensureCombinedGroup()
            team == null -> ensureSpectatorsGroup()
            isUsingCombinedGroup() -> ensureCombinedGroup()
            // if the game is _not_ using a combined group, move winning teams to spectator
            team.isWinner() -> ensureSpectatorsGroup()
            else -> createTeamGroup(team)
        }
    }

    private fun resetGroups() {
        val combined = combinedGroup.also { combinedGroup = null }
        val spectators = spectatorsGroup.also { spectatorsGroup = null }
        val teams = teamGroups.values.toList()
        teamGroups.clear()
        usingCombinedGroup = null

        // Remove players from all groups first: voicechat never deletes a group that still has
        // players in it. Only the groups that already exist are touched - nothing is created here.
        playerManager.getPlayers().forEach { player ->
            combined?.removePlayer(player)
            spectators?.removePlayer(player)
            teams.forEach { it?.removePlayer(player) }
        }

        combined?.close()
        spectators?.close()
        teams.forEach { it?.close() }
    }

    private fun updateGroups(players: List<IPlayerHandle>) {
        for (player in players) {
            val team = teamService.getPlayerTeam(player)
            getGroup(team)?.addPlayer(player)
        }
    }

    private fun onStateChange() {
        // reset usingCombinedGroup on state change or between rounds
        usingCombinedGroup = null

        val playersToUpdate = when {
            state.isLobbyMode -> playerManager.getPlayers()
            else -> playerManager.getPlayers().filter { teamService.isPlaying(it) }
        }
        updateGroups(playersToUpdate)
    }

    init {
        scopedEvents.onEnter(GameState.STARTING) {
            onStateChange()
        }

        scopedEvents.onEnter(GameState.POSTGAME) {
            onStateChange()
            // Clean up the team groups, which should now be unused
            teamGroups.values.forEach { it?.close() }
            teamGroups.clear()
        }

        scopedEvents.onEnter(GameState.PREGAME) {
            resetGroups()
        }

        scopedEvents.onChangeTeam { (player) ->
            playerManager.getPlayer(player.uuid)
                ?.let { updateGroups(listOf(it)) }
        }

        scopedEvents.onPlayerJoin { (player) ->
            updateGroups(listOf(player))
        }

        eventBus.register(TeamWinnerEvent) { (team) ->
            team.players
                .mapNotNull { playerManager.getPlayer(it.uuid) }
                .let { updateGroups(it) }
        }
    }
}
