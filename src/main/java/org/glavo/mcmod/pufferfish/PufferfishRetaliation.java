// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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

    /// Equipment effects follow successful intrinsic or enchanted thorns, including damage absorbed by absorption hearts.
    public static void effectsOnThorns(LivingDamageEvent.Post event) {
        var target = event.getEntity();
        if (target.level().isClientSide() || !event.getSource().is(DamageTypes.THORNS)
                || !(event.getSource().getEntity() instanceof LivingEntity wearer) || wearer == target || wearer.isSpectator()
                || !dealtDamage(event)) return;
        var poison = PufferfishArmor.poison(wearer);
        if (PufferfishHeadgear.isWearing(wearer)) {
            poison = PufferfishPoison.strongest(poison, PufferfishPoison.of(wearer.getItemBySlot(EquipmentSlot.HEAD)));
        }
        if (poison != null) poison.apply(target, wearer, PufferfishCombat.STATUS_EFFECT_DURATION_TICKS);
        if (PufferfishHeadgear.isWearing(wearer) && PufferfishItems.isRadiant(wearer.getItemBySlot(EquipmentSlot.HEAD))
                || PufferfishArmor.isWearingRadiant(wearer)) {
            target.addEffect(PufferfishCombat.passiveEffect(new ItemStack(PufferfishItems.RADIANT.get())), wearer);
            PufferfishCombat.applyRadiantGlow(target, wearer);
        }
    }
}
