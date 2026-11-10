// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class PufferfishRetaliation {
    private PufferfishRetaliation() {}

    /// Equipment poison follows successful intrinsic or enchanted thorns, including damage absorbed by absorption hearts.
    public static void poisonOnThorns(LivingDamageEvent.Post event) {
        var target = event.getEntity();
        if (target.level().isClientSide() || !event.getSource().is(DamageTypes.THORNS)
                || !(event.getSource().getEntity() instanceof LivingEntity wearer) || wearer == target || wearer.isSpectator()
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        var poison = PufferfishArmor.poison(wearer);
        if (PufferfishHeadgear.isWearing(wearer)) {
            var headEffect = PufferfishCombat.passiveEffect(wearer.getItemBySlot(EquipmentSlot.HEAD));
            if (headEffect != null && headEffect.is(MobEffects.POISON)
                    && (poison == null || headEffect.getDuration() > poison.getDuration())) poison = headEffect;
        }
        if (poison != null) target.addEffect(new MobEffectInstance(MobEffects.POISON, poison.getDuration(), 0), wearer);
    }
}
