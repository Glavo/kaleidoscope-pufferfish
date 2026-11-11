// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;

public final class PufferfishMobThorns {
    private PufferfishMobThorns() {}

    public static void retaliate(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof MaterialPufferfish fish) || !(fish.level() instanceof ServerLevel level)) return;
        if (!PufferfishRetaliation.canRetaliate(event)) return;
        int strength = PufferfishVariant.of(fish.fish()).material().mobThorns();
        PufferfishRetaliation.thorns(level, strength,
                new EnchantedItemInUse(fish.fish().copy(), null, fish, item -> {}), fish, event.getSource());
    }
}
