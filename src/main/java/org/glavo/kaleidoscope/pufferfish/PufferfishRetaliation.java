// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class PufferfishRetaliation {
    private PufferfishRetaliation() {}

    /// Equipment retaliation is balanced independently of weapon attacks and placed-fish contact.
    @javax.annotation.Nullable
    public static MobEffectInstance effect(ItemStack stack) {
        if (PufferfishItems.isRadiant(stack)) return new MobEffectInstance(MobEffects.WITHER, 120, 0);
        int copper = CopperPufferfish.index(stack);
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (copper < 0 && id.getNamespace().equals(KaleidoscopePufferfish.MODID)) {
            for (int stage = 0; stage < PufferfishItems.COPPER_VARIANTS.size(); stage++) {
                if (id.getPath().startsWith(PufferfishItems.COPPER_VARIANTS.get(stage).getId().getPath() + "_skin_")) {
                    copper = stage;
                    break;
                }
            }
        }
        if (copper >= 4) return null;
        if (copper > 0) return new MobEffectInstance(MobEffects.POISON, copper * 120, copper - 1);
        boolean durable = PufferfishItems.isNetherite(stack) || PufferfishItems.isHeavy(stack)
                || id.getNamespace().equals(KaleidoscopePufferfish.MODID)
                && (id.getPath().startsWith("netherite_pufferfish_skin_") || id.getPath().startsWith("heavy_pufferfish_skin_"));
        return new MobEffectInstance(MobEffects.POISON, durable ? 100 : 60, 0);
    }

    @javax.annotation.Nullable
    public static MobEffectInstance strongestPoison(@javax.annotation.Nullable MobEffectInstance first,
            @javax.annotation.Nullable MobEffectInstance second) {
        if (second == null || !second.is(MobEffects.POISON)) return first;
        return first == null || second.getAmplifier() > first.getAmplifier()
                || second.getAmplifier() == first.getAmplifier() && second.getDuration() > first.getDuration() ? second : first;
    }

    /// Equipment effects follow successful intrinsic or enchanted thorns, including damage absorbed by absorption hearts.
    public static void effectsOnThorns(LivingDamageEvent.Post event) {
        var target = event.getEntity();
        if (target.level().isClientSide() || !event.getSource().is(DamageTypes.THORNS)
                || !(event.getSource().getEntity() instanceof LivingEntity wearer) || wearer == target || wearer.isSpectator()
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        var poison = PufferfishArmor.poison(wearer);
        if (PufferfishHeadgear.isWearing(wearer)) {
            poison = strongestPoison(poison, effect(wearer.getItemBySlot(EquipmentSlot.HEAD)));
        }
        if (poison != null) target.addEffect(poison, wearer);
        if (PufferfishHeadgear.isWearing(wearer) && PufferfishItems.isRadiant(wearer.getItemBySlot(EquipmentSlot.HEAD))
                || PufferfishArmor.isWearingRadiant(wearer)
                || wearer.isBlocking() && wearer.getUseItem().is(PufferfishShields.VARIANTS.get("radiant_pufferfish_skin_shield").get())) {
            target.addEffect(effect(new ItemStack(PufferfishItems.RADIANT.get())), wearer);
            PufferfishCombat.applyRadiantGlow(target, wearer);
        }
    }
}
