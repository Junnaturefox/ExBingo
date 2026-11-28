package me.jfenn.bingo.common.ready

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.packet.PacketConverter
import net.minecraft.resources.ResourceLocation

/** Client -> server: the player voted for a post-game action. */
class SetVotePacket(
    val option: VoteType,
) {
    object V1 : PacketConverter<SetVotePacket> {
        override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID_BINGO, "set_vote")

        override fun toPacketBuf(source: SetVotePacket, dest: IPacketBuf) {
            dest.writeString(source.option.name)
        }

        override fun fromPacketBuf(buf: IPacketBuf): SetVotePacket {
            val option = try {
                VoteType.valueOf(buf.readString())
            } catch (e: IllegalArgumentException) {
                VoteType.PREGAME
            }
            return SetVotePacket(option)
        }
    }
}
