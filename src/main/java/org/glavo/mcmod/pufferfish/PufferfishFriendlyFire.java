// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pufferfish;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

public final class PufferfishFriendlyFire {
    private PufferfishFriendlyFire() {}

    private static boolean isOtherFish(Entity source, Pufferfish target) {
        return source instanceof Pufferfish && source != target;
    }

    public static void damage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Pufferfish target
                && (isOtherFish(event.getSource().getDirectEntity(), target) || isOtherFish(event.getSource().getEntity(), target))) {
            event.setCanceled(true);
        }
    }

    public static void effect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Pufferfish target && isOtherFish(event.getEffectSource(), target)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
