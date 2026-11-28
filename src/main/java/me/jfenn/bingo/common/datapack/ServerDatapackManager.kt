package me.jfenn.bingo.common.datapack

import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.lobby.LobbyModeTarget
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.mixinhandler.LobbyDatapackRequest
import me.jfenn.bingo.platform.IModEnvironment
import org.slf4j.Logger
import kotlin.io.path.deleteIfExists

/**
 * Installs (or removes) the datapack that provides the lobby dimension.
 *
 * The datapack lives inside the save folder, so lobby mode itself is per save; the request that
 * asks for it is stored in the global server config. That request is bound to the save it was made
 * for ([me.jfenn.bingo.common.config.ServerConfig.isLobbyModeWorld]) - otherwise enabling lobby mode
 * in one world would install the datapack into whichever save the server loads next, turning lobby
 * mode on there as well.
 */
class ServerDatapackManager(
    log: Logger,
    private val configService: ConfigService,
    environment: IModEnvironment,
    private val serverProps: ServerProps,
    private val lobbyWorldService: LobbyWorldService,
) : BingoComponent() {

    init {
        // If the env is on a dedicated server, we need to install the datapack manually
        if (environment.envType == IModEnvironment.EnvType.SERVER) {
            val server = configService.config.server
            val worldName = serverProps.levelName

            // Configs written before the request carried a save name belong to the save being
            // started now; bind them so switching worlds later cannot pick the request up.
            if (server.isLobbyMode && server.isLobbyModeWorld == null) {
                log.info("[ServerDatapackManager] Binding the existing isLobbyMode=true request to the save '$worldName'")
                server.isLobbyModeWorld = worldName
                configService.writeConfig(configService.config)
            }

            val datapackPath = environment.gameDir.resolve("$worldName/datapacks/$MOD_ID.zip")

            // tell the pack-configuration mixin whether the lobby datapack has to be enabled: the
            // server reads its enabled pack list from level.dat when it starts, so an installed
            // datapack alone does not guarantee that the lobby dimension loads
            LobbyDatapackRequest.packId = if (server.isLobbyMode && LobbyModeTarget.appliesTo(server, worldName)) {
                "file/$MOD_ID.zip"
            } else {
                null
            }

            when {
                !LobbyModeTarget.appliesTo(server, worldName) -> {
                    log.info(
                        "[ServerDatapackManager] Lobby mode was requested for the save '{}', not '{}' - leaving this save untouched",
                        server.isLobbyModeWorld,
                        worldName,
                    )
                }

                server.isLobbyMode -> {
                    log.info("[ServerDatapackManager] isLobbyMode=true - Installing the $MOD_ID datapack in $datapackPath")
                    lobbyWorldService.copyDataPack(datapackPath)
                }

                else -> {
                    log.info("[ServerDatapackManager] isLobbyMode=false - Verifying that $datapackPath does not exist...")
                    if (datapackPath.deleteIfExists()) {
                        log.warn("[ServerDatapackManager] Deleted $datapackPath")
                    }

                    // the removal request is fulfilled - forget which save it was bound to
                    if (server.isLobbyModeWorld != null) {
                        server.isLobbyModeWorld = null
                        configService.writeConfig(configService.config)
                    }
                }
            }
        }
    }

}
