// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;

public final class PufferfishFriendlyFire {
    private PufferfishFriendlyFire() {}

    private static boolean isOtherFish(Entity source, Pufferfish target) {
        return source instanceof Pufferfish && source != target;
    }

    public static void damage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Cat && isPufferfishDamage(event.getSource())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof Pufferfish target
                && (isOtherFish(event.getSource().getDirectEntity(), target) || isOtherFish(event.getSource().getEntity(), target))) {
            event.setCanceled(true);
        }
    }

    public static void effect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Cat && isPufferfishSource(event.getEffectSource())
                || event.getEntity() instanceof Pufferfish target && isOtherFish(event.getEffectSource(), target)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    public static void attack(AttackEntityEvent event) {
        if (event.getTarget() instanceof Cat && PufferfishItems.isWeapon(event.getEntity().getMainHandItem())) event.setCanceled(true);
    }

    public static void lightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof Cat && event.getLightning().getPersistentData().getBoolean(PufferfishChanneling.LIGHTNING_MARKER)) {
            event.setCanceled(true);
        }
    }

    private static boolean isPufferfishSource(Entity entity) {
        return entity instanceof Pufferfish || entity instanceof ThrownPufferfish || entity instanceof PufferfishFlailHead
                || entity instanceof FallingBlockEntity falling && falling.getBlockState().getBlock() instanceof HeavyPufferfishBlock;
    }

    private static boolean isPufferfishDamage(DamageSource source) {
        if (isPufferfishSource(source.getDirectEntity()) || isPufferfishSource(source.getEntity())
                || source.is(PufferfishContactDamage.DAMAGE_TYPE) || source.is(ThrownPufferfish.DAMAGE_TYPE)) return true;
        if (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) {
            var weapon = source.getWeaponItem();
            return weapon != null && PufferfishItems.isWeapon(weapon);
        }
        if (source.is(DamageTypes.THORNS) && source.getEntity() instanceof LivingEntity wearer) {
            if (PufferfishHeadgear.isWearing(wearer)) return true;
            for (var slot : EquipmentSlot.values()) {
                var stack = wearer.getItemBySlot(slot);
                if (stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == slot
                        && PufferfishVariant.of(stack) != null) return true;
            }
            var shield = wearer.getUseItem();
            return shield.getItem() instanceof ShieldItem && PufferfishVariant.of(shield) != null;
        }
        return false;
    }
}
