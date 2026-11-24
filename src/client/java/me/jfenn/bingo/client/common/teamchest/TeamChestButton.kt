package me.jfenn.bingo.client.common.teamchest

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.WidgetSprites
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * The team chest button shown in the survival inventory, next to the vanilla recipe book button.
 *
 * Its background sprite is the vanilla recipe book button with the book erased, so the frame is
 * pixel-identical to its neighbour; the icon is an actual item stack, rendered the same way the
 * game renders items anywhere else. That means the button shows the real copper chest from Vanilla
 * Backport (or the vanilla chest when that mod is absent) instead of a hand-drawn lookalike.
 */
internal class TeamChestButton(
    x: Int,
    y: Int,
    private val onPress: () -> Unit,
) : AbstractButton(
    x,
    y,
    WIDTH,
    HEIGHT,
    Component.translatable("bingo.teamchest.button"),
) {

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.blitSprite(SPRITES.get(isActive, isHoveredOrFocused), x, y, width, height)
        guiGraphics.renderItem(icon, x + ICON_OFFSET_X, y + ICON_OFFSET_Y)
    }

    override fun onPress() {
        onPress()
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput)
    }

    companion object {
        /** Same size as the vanilla recipe book button. */
        const val WIDTH = 20
        const val HEIGHT = 18

        /** The icon area of the vanilla button sprite (16x16 inside its frame). */
        private const val ICON_OFFSET_X = 2
        private const val ICON_OFFSET_Y = 1

        private val SPRITES = WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("minecraft", "bingo/team_chest"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "bingo/team_chest_highlighted"),
        )

        /**
         * Icon shown on the button.
         *
         * Vanilla Backport registers its backported content in the `minecraft` namespace, so the
         * copper chest is `minecraft:copper_chest` - the very same id upstream Minecraft uses since
         * 1.21.9. Falls back to the vanilla chest when the mod is not installed.
         */
        val icon: ItemStack by lazy {
            val copperChest = listOf("minecraft:copper_chest", "vanillabackport:copper_chest")
                .mapNotNull { ResourceLocation.tryParse(it) }
                .firstNotNullOfOrNull { id ->
                    BuiltInRegistries.ITEM.get(id)
                        ?.takeIf { it != Items.AIR }
                        ?.let { item -> ItemStack(item) }
                }

            copperChest ?: ItemStack(Items.CHEST)
        }
    }
}
