package me.jfenn.bingo.platform.packet

import me.jfenn.bingo.platform.IPacketBuf
import net.minecraft.resources.ResourceLocation

/**
 * One wire format of a packet. Several implementations of the same packet live side by side so a
 * server can keep talking to slightly older clients.
 *
 * Version policy: keep **the current version and the previous one** (`Vn`, `Vn-1`). Each converter
 * registers its own payload channel, so the previous version is the compatibility floor - clients
 * older than that fall back to the non-mod path (vanilla title / dialog / hotbar message). When a
 * packet changes shape, add a new `Vn+1` and delete the oldest one in the same commit:
 * 1. delete the `object Vx` block,
 * 2. drop its `registerS2C` line on both sides (`ServerPacketEvents`, `ClientPacketEvents`),
 * 3. drop its client `eventBus.register(...Vx, ...)` subscriptions,
 * 4. drop its branch from the send chain in the sending controller.
 */
interface PacketConverter<T> {

    val id: ResourceLocation

    fun toPacketBuf(source: T, dest: IPacketBuf)

    fun fromPacketBuf(buf: IPacketBuf): T

}
