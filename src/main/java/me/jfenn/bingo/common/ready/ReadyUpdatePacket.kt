package me.jfenn.bingo.common.ready

import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.packet.PacketConverter
import net.minecraft.resources.ResourceLocation
import java.time.Duration

data class ReadyUpdatePacket(
    val isRunning: Boolean,
    val isReady: Boolean,
    val state: GameState,
    val remainingDuration: Duration,
    val totalDuration: Duration,
    val readyPlayers: Int,
    val totalPlayers: Int,
    val title: IText?,
    val subtitle: IText?,
    val canSendReady: Boolean = false,
    /** Vote tallies for the post-game action; empty when there is no vote running. */
    val votes: Map<VoteType, Int> = emptyMap(),
    /** This player's own vote, if any. */
    val myVote: VoteType? = null,
) {
    object V3 : PacketConverter<ReadyUpdatePacket> {
        override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID_BINGO, "ready_timer_update_v3")

        override fun fromPacketBuf(buf: IPacketBuf): ReadyUpdatePacket {
            return ReadyUpdatePacket(
                isRunning = buf.readBoolean(),
                isReady = buf.readBoolean(),
                state = try {
                    GameState.valueOf(buf.readString())
                } catch (e: IllegalArgumentException) {
                    GameState.PREGAME
                },
                remainingDuration = buf.readDuration(),
                totalDuration = buf.readDuration(),
                readyPlayers = buf.readInt(),
                totalPlayers = buf.readInt(),
                title = buf.readNullable(buf::readText),
                subtitle = buf.readNullable(buf::readText),
                canSendReady = buf.readBoolean(),
            )
        }

        override fun toPacketBuf(source: ReadyUpdatePacket, dest: IPacketBuf) {
            dest.writeBoolean(source.isRunning)
            dest.writeBoolean(source.isReady)
            dest.writeString(source.state.name)
            dest.writeDuration(source.remainingDuration)
            dest.writeDuration(source.totalDuration)
            dest.writeInt(source.readyPlayers)
            dest.writeInt(source.totalPlayers)
            dest.writeNullable(source.title, dest::writeText)
            dest.writeNullable(source.subtitle, dest::writeText)
            dest.writeBoolean(source.canSendReady)
        }
    }

    object V4 : PacketConverter<ReadyUpdatePacket> {
        override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID_BINGO, "ready_timer_update_v4")

        override fun fromPacketBuf(buf: IPacketBuf): ReadyUpdatePacket {
            val base = V3.fromPacketBuf(buf)
            val votes = buf.readList {
                val type = try {
                    VoteType.valueOf(buf.readString())
                } catch (e: IllegalArgumentException) {
                    VoteType.PREGAME
                }
                type to buf.readInt()
            }.toMap()
            val myVote = buf.readNullable {
                try {
                    VoteType.valueOf(buf.readString())
                } catch (e: IllegalArgumentException) {
                    VoteType.PREGAME
                }
            }
            return ReadyUpdatePacket(
                isRunning = base.isRunning,
                isReady = base.isReady,
                state = base.state,
                remainingDuration = base.remainingDuration,
                totalDuration = base.totalDuration,
                readyPlayers = base.readyPlayers,
                totalPlayers = base.totalPlayers,
                title = base.title,
                subtitle = base.subtitle,
                canSendReady = base.canSendReady,
                votes = votes,
                myVote = myVote,
            )
        }

        override fun toPacketBuf(source: ReadyUpdatePacket, dest: IPacketBuf) {
            V3.toPacketBuf(source, dest)
            dest.writeList(source.votes.entries) { (type, count) ->
                dest.writeString(type.name)
                dest.writeInt(count)
            }
            dest.writeNullable(source.myVote) { dest.writeString(it.name) }
        }
    }
}
