package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.platform.item.IItemStack

/**
 * Backs the cached [IItemStack] wrapper that [me.jfenn.bingo.mixin.ItemStackMixin] stores on each
 * `ItemStack`, so the factory does not have to wrap the same stack over and over again.
 */
interface ItemStackMixinAccess {
    fun exbingoGetItemStackImpl(): IItemStack?
    fun exbingoSetItemStackImpl(impl: IItemStack?)
}
