package me.jfenn.bingo.common.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.awt.Color

typealias ColorType = @Serializable(with = ColorSerializer::class) Color

object ColorSerializer : KSerializer<Color> {
    override val descriptor = PrimitiveSerialDescriptor("java.awt.Color", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Color {
        val str = decoder.decodeString().removePrefix("#")
        // The bundled resource packs use #AARRGGBB (e.g. "#80505050"), so alpha must survive both
        // directions; `Color(int, true)` reads the color as AARRGGBB.
        val argb = when (str.length) {
            3 -> 0xFF000000.toInt() or expandShortHex(str)
            4 -> expandArgbShortHex(str)
            6 -> 0xFF000000.toInt() or Integer.parseInt(str, 16)
            8 -> Integer.parseUnsignedInt(str, 16).toInt()
            else -> throw IllegalArgumentException("Invalid color: #$str")
        }
        return Color(argb, true)
    }

    override fun serialize(encoder: Encoder, value: Color) {
        // getRGB() keeps the alpha channel; the old `value.rgb` always forced it to 0xFF.
        encoder.encodeString("#%08x".format(value.getRGB()))
    }

    private fun expandShortHex(str: String): Int {
        val r = Integer.parseInt(str.substring(0, 1), 16)
        val g = Integer.parseInt(str.substring(1, 2), 16)
        val b = Integer.parseInt(str.substring(2, 3), 16)
        return (r * 17 shl 16) or (g * 17 shl 8) or (b * 17)
    }

    private fun expandArgbShortHex(str: String): Int {
        fun nibble(index: Int) = Integer.parseInt(str.substring(index, index + 1), 16) * 17
        return (nibble(0) shl 24) or (nibble(1) shl 16) or (nibble(2) shl 8) or nibble(3)
    }
}
