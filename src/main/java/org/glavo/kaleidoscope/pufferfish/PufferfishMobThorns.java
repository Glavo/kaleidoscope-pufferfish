// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.glavo.kaleidoscope.pufferfish.entity.MaterialPufferfish;

public final class PufferfishMobThorns {
    private PufferfishMobThorns() {}

    public static void retaliate(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof MaterialPufferfish fish) || !(fish.level() instanceof ServerLevel level)) return;
        var source = event.getSource();
        if (source.is(DamageTypes.THORNS) || !(source.getEntity() instanceof LivingEntity attacker)
                || attacker == fish || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        int strength = switch (fish.kind()) {
            case COPPER, IRON, GOLDEN -> 1;
            case DIAMOND, NETHERITE -> 2;
            case RADIANT, HEAVY -> 3;
        };
        var thorns = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.THORNS);
        thorns.value().doPostAttack(level, strength,
                new EnchantedItemInUse(fish.fish().copy(), null, fish, item -> {}),
                EnchantmentTarget.VICTIM, fish, source);
    }
}
