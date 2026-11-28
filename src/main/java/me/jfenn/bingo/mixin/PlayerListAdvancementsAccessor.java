package me.jfenn.bingo.mixin;

import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

/**
 * Exposes the {@code PlayerList.advancements} map so the fake offline players created by
 * {@code PlayerManager.getOfflinePlayer} can be removed from it again - otherwise every offline
 * player queried by the objective/scoring code leaves a {@link PlayerAdvancements} entry (which
 * also holds the dummy player) in the vanilla map for the lifetime of the server.
 */
@Mixin(PlayerList.class)
public interface PlayerListAdvancementsAccessor {

    @Accessor("advancements")
    Map<UUID, PlayerAdvancements> getAdvancements();
}
