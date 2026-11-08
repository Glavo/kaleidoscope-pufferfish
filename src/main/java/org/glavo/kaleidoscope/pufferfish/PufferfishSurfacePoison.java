// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.google.common.base.Suppliers;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/// Poisons living entities standing on pufferfish blocks or floor-mounted skins.
public final class PufferfishSurfacePoison {
    /// The full pufferfish blocks, resolved after registration on the first standing-entity tick.
    private static final Supplier<Set<Block>> BLOCKS = Suppliers.memoize(() -> {
        var blocks = new HashSet<Block>();
        blocks.add(PufferfishBlocks.get(null));
        blocks.add(PufferfishBlocks.NETHERITE.get());
        for (var color : DyeColor.values()) blocks.add(PufferfishBlocks.get(color));
        return Set.copyOf(blocks);
    });

    /// Prevents construction of this event handler.
    private PufferfishSurfacePoison() {}

    /// Applies six seconds of Poison I on a poisonous support surface on the server.
    /// Refreshes Poison I when at most three seconds remain, allowing its damage timer to advance.
    /// Stronger and infinite poison effects are retained; airborne entities and spectators are ignored.
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()
                || !entity.isAlive() || !entity.onGround() || entity.isSpectator()) return;
        var state = entity.level().getBlockState(entity.getOnPos());
        boolean skin = state.getBlock() instanceof PufferfishSkinBlock
                && state.getValue(PufferfishSkinBlock.FACE) == AttachFace.FLOOR;
        if (!skin && !BLOCKS.get().contains(state.getBlock())) return;
        var poison = entity.getEffect(MobEffects.POISON);
        if (poison == null || poison.getAmplifier() == 0 && !poison.isInfiniteDuration() && poison.getDuration() <= 60) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
        }
    }
}
