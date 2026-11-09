// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/// Poisons living entities standing on non-golden floor-mounted pufferfish skins.
public final class PufferfishSurfacePoison {
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
                && !state.is(PufferfishBlocks.GOLDEN_SKIN.get())
                && state.getValue(PufferfishSkinBlock.FACE) == AttachFace.FLOOR;
        if (!skin) return;
        var poison = entity.getEffect(MobEffects.POISON);
        if (poison == null || poison.getAmplifier() == 0 && !poison.isInfiniteDuration() && poison.getDuration() <= 60) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
        }
    }
}
