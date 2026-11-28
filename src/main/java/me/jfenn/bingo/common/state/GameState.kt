package me.jfenn.bingo.common.state

enum class GameState(
    val color: String,
    val motd: String,
) {
    /** This state shouldn't show up in actual use; it's immediately changed from the default in ScopeManager */
    UNINITIALIZED("§a", "PRE-GAME"),
    PREGAME("§a", "PRE-GAME"),
    STARTING("§a", "PRE-GAME"),
    PRELOADING("§a", "PRE-GAME"),
    LOADING("§c", "IN-GAME"),
    COUNTDOWN("§c", "IN-GAME"),
    PLAYING("§c", "IN-GAME"),
    POSTGAME("§6", "POST-GAME"),

    /** Used when "Next Round" is clicked on the end menu */
    STARTING_NEXT_ROUND("§6", "POST-GAME");

    val isPlayingOrCountdown get() = when (this) {
        LOADING, COUNTDOWN, PLAYING -> true
        else -> false
    }

    val isActiveGame get() = when (this) {
        STARTING, PRELOADING, LOADING, COUNTDOWN, PLAYING -> true
        else -> false
    }

    /** True in the states from which a new round may be started. */
    val canStartNewRound get() = when (this) {
        PREGAME, STARTING_NEXT_ROUND -> true
        else -> false
    }
}
