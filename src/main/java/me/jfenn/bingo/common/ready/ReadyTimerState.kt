package me.jfenn.bingo.common.ready

import me.jfenn.bingo.common.utils.minus
import me.jfenn.bingo.platform.IPlayerHandle
import java.time.Duration
import java.time.Instant
import java.util.*

class ReadyTimerState {

    private var totalPlayers = listOf<UUID>()
    private val readyPlayers = mutableSetOf<UUID>()
    private val votes = mutableMapOf<UUID, VoteType>()
    private var startedAt: Instant? = null
    private var totalDuration: Duration? = null

    /**
     * If the timer is cancelled with /ready cancel
     * Don't start it again until /ready is used
     */
    var isCancelled: Boolean = false

    val isRunning: Boolean get() = startedAt != null

    fun updatePlayers(players: List<IPlayerHandle>) {
        totalPlayers = players.map { it.uuid }
    }

    val readyCount get() = readyPlayers.count { it in totalPlayers }
    val playerCount get() = totalPlayers.size

    fun isReady(player: UUID): Boolean = player in readyPlayers

    fun setVote(player: UUID, vote: VoteType) {
        votes[player] = vote
    }

    fun vote(player: UUID): VoteType? = votes[player]

    fun voteCounts(): Map<VoteType, Int> = VoteType.entries.associateWith { type ->
        votes.count { (uuid, value) -> value == type && uuid in totalPlayers }
    }

    /** The most-voted option (plurality), or null when nobody voted or the top count is tied. */
    fun winningVote(): VoteType? = voteCounts()
        .entries
        .maxByOrNull { it.value }
        ?.takeIf { (_, count) ->
            count > 0 && voteCounts().values.count { it == count } == 1
        }
        ?.key

    fun setReady(player: UUID, ready: Boolean) {
        if (ready) {
            readyPlayers.add(player)
        } else {
            readyPlayers.remove(player)
        }
    }

    fun startTimer(duration: Duration) {
        isCancelled = false
        startedAt = Instant.now()
        totalDuration = duration
    }

    fun duration(): Duration? {
        val readyRatio = totalPlayers.count { isReady(it) } / totalPlayers.size.toFloat().coerceAtLeast(1f)
        val millis = (1f - readyRatio) * (totalDuration?.toMillis() ?: return null)
        return Duration.ofMillis(millis.toLong())
    }

    fun totalTime(): Duration? = totalDuration

    fun elapsedTime(): Duration? {
        val now = Instant.now()
        return now - (startedAt ?: return null)
    }

    fun remainingTime(): Duration? {
        val totalDuration = duration() ?: return null
        val elapsedDuration = elapsedTime() ?: return null
        return totalDuration - elapsedDuration
    }

    fun reset() {
        readyPlayers.clear()
        votes.clear()
        startedAt = null
        totalDuration = null
        isCancelled = false
    }

}
