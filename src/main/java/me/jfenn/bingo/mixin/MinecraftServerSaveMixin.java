package me.jfenn.bingo.mixin;

import me.jfenn.bingo.mixinhandler.ServerEventMixinHandler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Emits {@code ServerEvent.Saved} after vanilla writes the world, so the bingo state is persisted
 * on autosave as well (it used to be written only on state changes and on shutdown, which lost the
 * progress of a running round if the server crashed).
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerSaveMixin {

    @Inject(method = "saveEverything", at = @At("RETURN"))
    private void bingo$onSaved(boolean suppressLog, boolean flush, boolean forced, CallbackInfoReturnable<Boolean> cir) {
        ServerEventMixinHandler.onSaved((MinecraftServer) (Object) this);
    }
}
