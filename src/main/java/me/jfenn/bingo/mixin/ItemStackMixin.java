package me.jfenn.bingo.mixin;

import me.jfenn.bingo.mixinhandler.ItemStackMixinAccess;
import me.jfenn.bingo.platform.item.IItemStack;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Caches the {@link IItemStack} wrapper for this stack instance.
 *
 * Item objective matching walks every inventory slot of every player a few times per second, and
 * each walk used to allocate a wrapper object per stack (plus the captured factory reference of the
 * inner class). The wrapper only reads live state from the stack, so one instance can be reused.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin implements ItemStackMixinAccess {

    @Unique
    private IItemStack exbingo$itemStackImpl;

    @Override
    public IItemStack exbingoGetItemStackImpl() {
        return this.exbingo$itemStackImpl;
    }

    @Override
    public void exbingoSetItemStackImpl(IItemStack impl) {
        this.exbingo$itemStackImpl = impl;
    }
}
