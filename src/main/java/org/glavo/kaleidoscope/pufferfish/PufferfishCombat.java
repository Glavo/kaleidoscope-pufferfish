// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

public final class PufferfishCombat {
    private PufferfishCombat() {}

    private static int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.getEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(key))
                .mapToInt(entry -> entry.getIntValue()).findFirst().orElse(0);
    }

    public static float damage(ItemStack stack) {
        int density = enchantmentLevel(stack, Enchantments.DENSITY);
        return (PufferfishItems.isNetherite(stack) ? 10 : 2) + (density > 0 ? 0.5F + 0.5F * density : 0);
    }

    public static void attributes(ItemAttributeModifierEvent event) {
        if (!PufferfishItems.isPufferfish(event.getItemStack())) return;
        event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                damage(event.getItemStack()) - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                -3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    public static void meleeHit(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(event.getSource().getDirectEntity() instanceof Player player)
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        var stack = player.getMainHandItem();
        if (PufferfishItems.isPufferfish(stack)) {
            hitEffects(stack, event.getEntity(), player, event.getEntity().position().subtract(player.position()));
        }
    }

    public static void hitEffects(ItemStack stack, LivingEntity target, Entity source, Vec3 direction) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, PufferfishItems.isNetherite(stack) ? 1 : 0), source);
        int punch = enchantmentLevel(stack, Enchantments.PUNCH);
        if (punch > 0) {
            double resistance = Math.max(0, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 push = direction.multiply(1, 0, 1).normalize().scale(punch * 0.6 * resistance);
            if (push.lengthSqr() > 0) target.push(push.x, 0.1, push.z);
        }
    }
}
