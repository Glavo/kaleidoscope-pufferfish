// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/// Calculates pufferfish weapon damage and applies shared melee and projectile effects.
public final class PufferfishCombat {
    /// Prevents construction of this combat utility.
    private PufferfishCombat() {}

    /// Returns the stored level of the specified enchantment, or zero when absent.
    private static int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.getEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(key))
                .mapToInt(entry -> entry.getIntValue()).findFirst().orElse(0);
    }

    /// Returns projectile or flail damage with the flat Density bonus; handheld fish melee uses separate rules.
    public static float damage(ItemStack stack) {
        int density = enchantmentLevel(stack, Enchantments.DENSITY);
        float base = PufferfishItems.isFlail(stack) ? (PufferfishItems.hasNetheriteCombat(stack) ? 12 : 6)
                : (PufferfishItems.hasNetheriteCombat(stack) ? 10 : 2);
        return base + (density > 0 ? 0.5F + 0.5F * density : 0);
    }

    /// Applies target-dependent enchantment damage using the projectile's carried weapon, independently of the owner's current hand.
    public static float projectileDamage(ServerLevel level, ItemStack stack, Entity target, DamageSource source) {
        return EnchantmentHelper.modifyDamage(level, stack, target, source, damage(stack));
    }

    /// Adds main-hand weapon attributes, applying configured base damage only to handheld fish.
    public static void attributes(ItemAttributeModifierEvent event) {
        if (!PufferfishItems.isWeapon(event.getItemStack())) return;
        var stack = event.getItemStack();
        float meleeDamage = PufferfishItems.isPufferfish(stack) ? (float) (6 * meleeMultiplier(stack)) : damage(stack);
        event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                meleeDamage - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                PufferfishItems.isHeavy(stack) ? -3.4 : -3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    /// Returns whether the supplied fish can perform a configured smash with the attacker's current fall state.
    public static boolean canSmash(LivingEntity attacker, ItemStack stack) {
        return PufferfishItems.isPufferfish(stack) && PufferfishCombatConfig.value(PufferfishCombatConfig.SMASH_ENABLED)
                && MaceItem.canSmashAttack(attacker);
    }

    /// Returns the fish variant's configured multiplier for unenchanted melee damage.
    private static double meleeMultiplier(ItemStack stack) {
        return PufferfishCombatConfig.value(PufferfishItems.isHeavy(stack) ? PufferfishCombatConfig.HEAVY_DAMAGE_MULTIPLIER : PufferfishItems.isNetherite(stack)
                ? PufferfishCombatConfig.NETHERITE_DAMAGE_MULTIPLIER : PufferfishCombatConfig.NORMAL_DAMAGE_MULTIPLIER);
    }

    /// Returns the capped falling bonus, including Density, before critical hits and armor.
    /// The caller must supply a nonnegative fall distance and a fish performing a smash attack.
    public static float smashBonus(ItemStack stack, float distance) {
        double curve = Math.min(distance, 3) * PufferfishCombatConfig.value(PufferfishCombatConfig.FIRST_THREE_BLOCKS_DAMAGE)
                + Math.min(Math.max(distance - 3, 0), 5) * PufferfishCombatConfig.value(PufferfishCombatConfig.NEXT_FIVE_BLOCKS_DAMAGE)
                + Math.max(distance - 8, 0) * PufferfishCombatConfig.value(PufferfishCombatConfig.REMAINING_BLOCKS_DAMAGE);
        double density = PufferfishCombatConfig.value(PufferfishItems.isHeavy(stack) ? PufferfishCombatConfig.HEAVY_DENSITY_DAMAGE : PufferfishItems.isNetherite(stack)
                ? PufferfishCombatConfig.NETHERITE_DENSITY_DAMAGE : PufferfishCombatConfig.NORMAL_DENSITY_DAMAGE);
        double bonus = curve * meleeMultiplier(stack) + distance * enchantmentLevel(stack, Enchantments.DENSITY) * density;
        double cap = PufferfishCombatConfig.value(PufferfishCombatConfig.MAX_SMASH_BONUS);
        return (float) (cap > 0 ? Math.min(bonus, cap) : bonus);
    }

    /// Applies shared weapon effects after a server-side player attack damages health or absorption.
    public static void meleeHit(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(event.getSource().getDirectEntity() instanceof Player player)
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        var stack = player.getMainHandItem();
        if (PufferfishItems.isWeapon(stack)) {
            hitEffects(stack, event.getEntity(), player, event.getEntity().position().subtract(player.position()));
        }
    }

    /// Attempts to apply six seconds of poison and adds Punch knockback, respecting effect immunity and knockback resistance.
    public static void hitEffects(ItemStack stack, LivingEntity target, Entity source, Vec3 direction) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, PufferfishItems.hasNetheriteCombat(stack) ? 1 : 0), source);
        int punch = enchantmentLevel(stack, Enchantments.PUNCH);
        if (punch > 0) {
            double resistance = Math.max(0, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 push = direction.multiply(1, 0, 1).normalize().scale(punch * 0.6 * resistance);
            if (push.lengthSqr() > 0) target.push(push.x, 0.1, push.z);
        }
    }
}
