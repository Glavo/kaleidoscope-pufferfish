// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class PufferfishRetaliation {
    private PufferfishRetaliation() {}

    static boolean canRetaliate(LivingDamageEvent.Post event) {
        var source = event.getSource();
        return !source.is(DamageTypes.THORNS) && source.getEntity() instanceof LivingEntity attacker
                && attacker != event.getEntity() && dealtDamage(event);
    }

    private static boolean dealtDamage(LivingDamageEvent.Post event) {
        return event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) > 0;
    }

    static void thorns(ServerLevel level, int strength, EnchantedItemInUse item, LivingEntity defender, DamageSource source) {
        var thorns = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.THORNS);
        thorns.value().doPostAttack(level, strength, item, EnchantmentTarget.VICTIM, defender, source);
    }

    public static int thornsLevel(ItemStack stack) {
        var variant = PufferfishVariant.of(stack);
        if (variant == null) return 0;
        return PufferfishItems.isWeapon(stack) ? variant.material().headThorns() : variant.material().skinThorns();
    }

    /// Equipment retaliation is balanced independently of weapon attacks and placed-fish contact.
    @javax.annotation.Nullable
    public static MobEffectInstance effect(ItemStack stack) {
        var variant = PufferfishVariant.of(stack);
        if (variant == null || variant.waxed()) return null;
        if (variant.material() == PufferfishMaterial.RADIANT) return new MobEffectInstance(MobEffects.WITHER, 120, 0);
        int oxidation = variant.oxidation();
        if (oxidation > 0) return new MobEffectInstance(MobEffects.POISON, oxidation * 120, oxidation - 1);
        int duration = switch (variant.material()) {
            case NETHERITE, HEAVY -> 100;
            default -> 60;
        };
        return new MobEffectInstance(MobEffects.POISON, duration, 0);
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
                || !dealtDamage(event)) return;
        var poison = PufferfishArmor.poison(wearer);
        if (PufferfishHeadgear.isWearing(wearer)) {
            poison = strongestPoison(poison, effect(wearer.getItemBySlot(EquipmentSlot.HEAD)));
        }
        if (poison != null) target.addEffect(poison, wearer);
        if (PufferfishHeadgear.isWearing(wearer) && PufferfishItems.isRadiant(wearer.getItemBySlot(EquipmentSlot.HEAD))
                || PufferfishArmor.isWearingRadiant(wearer)) {
            target.addEffect(effect(new ItemStack(PufferfishItems.RADIANT.get())), wearer);
            PufferfishCombat.applyRadiantGlow(target, wearer);
        }
    }
}
