package me.jfenn.bingo.mixinhandler

/**
 * Communicates the lobby datapack that must be enabled to
 * [MinecraftServerConfigurePackRepositoryMixin] (via [DedicatedServerPackConfigDefaults]).
 *
 * The datapack is installed while the mod loads, but the server reads the enabled pack list out of
 * `level.dat` when it starts: if the pack was never recorded there - or was recorded as disabled by
 * another datapack manager - the lobby dimension silently fails to load and the server boots in
 * non-lobby mode even though lobby mode was requested for this save.
 */
object LobbyDatapackRequest {

    /**
     * Pack id (e.g. `file/exbingo.zip`) that must be force-enabled for the save being started, or
     * `null` when lobby mode is not requested for it.
     */
    @Volatile
    var packId: String? = null
}
