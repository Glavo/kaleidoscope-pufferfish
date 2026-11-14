// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/// Calculates pufferfish weapon damage and applies shared melee and projectile effects.
public final class PufferfishCombat {
    public static final int STATUS_EFFECT_DURATION_TICKS = 120;
    public static final int RADIANT_GLOW_DURATION_TICKS = 200;
    private static final float RADIANT_WITHER_CHANCE = 0.6F;
    private static final float RADIANT_DEBUFF_CHANCE = 0.05F;
    /// Prevents construction of this combat utility.
    private PufferfishCombat() {}

    /// Returns fish throwing or short flail cast damage with the flat Density bonus.
    public static float damage(ItemStack stack) {
        return throwingBaseDamage(stack) + densityBonus(stack);
    }

    /// Returns corresponding fish throwing damage with enchantments, before any flail charge multiplier.
    public static float projectileDamage(ServerLevel level, ItemStack stack, Entity target, DamageSource source) {
        float base = damage(stack);
        return EnchantmentHelper.modifyDamage(level, stack, target, source, base);
    }

    /// Adds main-hand weapon attributes, applying configured base damage only to handheld fish.
    public static void attributes(ItemAttributeModifierEvent event) {
        if (!PufferfishItems.isWeapon(event.getItemStack())) return;
        var stack = event.getItemStack();
        float meleeDamage = PufferfishItems.isHeavy(stack) ? (float) (6 * PufferfishCombatConfig.value(PufferfishCombatConfig.HEAVY_DAMAGE_MULTIPLIER)) : damage(stack);
        event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                meleeDamage - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        double speed = PufferfishItems.isFlail(stack) ? 20.0 / Math.max(1, PufferfishCombatConfig.value(PufferfishCombatConfig.THROW_COOLDOWN_TICKS))
                : PufferfishItems.hasHeavyCombat(stack) ? 0.6
                : PufferfishCombatConfig.value(PufferfishItems.hasNetheriteCombat(stack)
                ? PufferfishCombatConfig.NETHERITE_ATTACK_SPEED : PufferfishCombatConfig.NORMAL_ATTACK_SPEED);
        event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                speed - 4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    /// Returns whether the supplied fish can perform a configured smash with the attacker's current fall state.
    public static boolean canSmash(LivingEntity attacker, ItemStack stack) {
        return PufferfishItems.isHeavy(stack) && PufferfishCombatConfig.value(PufferfishCombatConfig.SMASH_ENABLED)
                && attacker.fallDistance > 1.5F && !attacker.isFallFlying();
    }

    /// Returns the capped heavy-fish falling bonus, including Density, before critical hits and armor.
    /// The caller must supply a nonnegative fall distance and a fish performing a smash attack.
    public static float smashBonus(ItemStack stack, float distance) {
        double curve = Math.min(distance, 3) * PufferfishCombatConfig.value(PufferfishCombatConfig.FIRST_THREE_BLOCKS_DAMAGE)
                + Math.min(Math.max(distance - 3, 0), 5) * PufferfishCombatConfig.value(PufferfishCombatConfig.NEXT_FIVE_BLOCKS_DAMAGE)
                + Math.max(distance - 8, 0) * PufferfishCombatConfig.value(PufferfishCombatConfig.REMAINING_BLOCKS_DAMAGE);
        double density = PufferfishCombatConfig.value(PufferfishCombatConfig.HEAVY_DENSITY_DAMAGE);
        double bonus = curve * PufferfishCombatConfig.value(PufferfishCombatConfig.HEAVY_DAMAGE_MULTIPLIER) + distance * enchantmentLevel(stack, Enchantments.DENSITY) * density;
        double cap = PufferfishCombatConfig.value(PufferfishCombatConfig.MAX_SMASH_BONUS);
        return (float) (cap > 0 ? Math.min(bonus, cap) : bonus);
    }

    /// Successful heavy-fish smashes protect the landing and repel nearby enemies, excluding cats.
    public static void smashHit(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof ServerPlayer player) || target instanceof Cat) return;
        var level = player.serverLevel();
        if (!player.isIgnoringFallDamageFromCurrentImpulse() || player.currentImpulseImpactPos == null
                || player.currentImpulseImpactPos.y > player.getY()) {
            player.currentImpulseImpactPos = player.position();
        }
        player.setIgnoreFallDamageFromCurrentImpulse(true);
        player.setDeltaMovement(player.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        var sound = SoundEvents.MACE_SMASH_AIR;
        if (target.onGround()) {
            player.setSpawnExtraParticlesOnFall(true);
            sound = player.fallDistance > 5 ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), 1, 1);
        level.levelEvent(2013, target.getOnPos(), 750);
        double radius = 3.5;
        for (var nearby : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius), entity ->
                entity != player && entity != target && !entity.isSpectator() && !(entity instanceof Cat)
                        && !player.isAlliedTo(entity)
                        && !(entity instanceof TamableAnimal pet && pet.isTame() && player.getUUID().equals(pet.getOwnerUUID()))
                        && !(entity instanceof ArmorStand stand && stand.isMarker()) && target.distanceToSqr(entity) <= radius * radius)) {
            var direction = nearby.position().subtract(target.position());
            double strength = (radius - direction.length()) * 0.7F * (player.fallDistance > 5 ? 2 : 1)
                    * (1 - nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (strength <= 0) continue;
            var impulse = direction.normalize().scale(strength);
            nearby.push(impulse.x, 0.7F, impulse.z);
            if (nearby instanceof ServerPlayer pushed) pushed.connection.send(new ClientboundSetEntityMotionPacket(pushed));
        }
    }

    /// Applies shared weapon effects after a server-side player attack damages health or absorption.
    public static void meleeHit(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(event.getSource().getDirectEntity() instanceof Player player)
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        var stack = player.getMainHandItem();
        if (PufferfishItems.isWeapon(stack)) {
            var poison = PufferfishPoison.of(stack);
            boolean applyEffect = PufferfishItems.isRadiant(stack) || PufferfishItems.isFlail(stack)
                    || poison != null && (poison.meleeChance() >= 1 || player.getRandom().nextFloat() < poison.meleeChance());
            hitEffects(stack, event.getEntity(), player, event.getEntity().position().subtract(player.position()), applyEffect);
        }
    }

    /// Applies the fish's active status effect and Punch knockback, respecting immunity and knockback resistance.
    public static void hitEffects(ItemStack stack, LivingEntity target, Entity source, Vec3 direction) {
        hitEffects(stack, target, source, direction, true);
    }

    /// Direct fish throws guarantee radiant wither while retaining normal bonus effects and Punch knockback.
    public static void thrownHitEffects(ItemStack stack, LivingEntity target, Entity source, Vec3 direction) {
        applyAttackEffect(stack, target, source, STATUS_EFFECT_DURATION_TICKS, true);
        hitEffects(stack, target, source, direction, false);
    }

    private static void hitEffects(ItemStack stack, LivingEntity target, Entity source, Vec3 direction, boolean applyEffect) {
        if (target instanceof Cat) return;
        if (applyEffect) applyAttackEffect(stack, target, source);
        int punch = enchantmentLevel(stack, Enchantments.PUNCH);
        if (punch > 0) {
            double resistance = Math.max(0, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 push = direction.multiply(1, 0, 1).normalize().scale(punch * 0.6 * resistance);
            if (push.lengthSqr() > 0) target.push(push.x, 0.1, push.z);
        }
    }

    /// Radiant hits roll Wither I or an upgrade to III; existing wither always refreshes.
    public static void applyAttackEffect(ItemStack stack, LivingEntity target, Entity source) {
        applyAttackEffect(stack, target, source, STATUS_EFFECT_DURATION_TICKS);
    }

    /// Uses the supplied poison or wither duration without changing radiant bonus effect durations.
    public static void applyAttackEffect(ItemStack stack, LivingEntity target, Entity source, int duration) {
        applyAttackEffect(stack, target, source, duration, false);
    }

    private static void applyAttackEffect(ItemStack stack, LivingEntity target, Entity source, int duration, boolean guaranteedWither) {
        if (target instanceof Cat) return;
        if (PufferfishItems.isRadiant(stack)) {
            var current = target.getEffect(MobEffects.WITHER);
            var random = source.getRandom();
            if (current == null) {
                if (guaranteedWither || random.nextFloat() < RADIANT_WITHER_CHANCE) {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration), source);
                }
            } else {
                int amplifier = current.getAmplifier();
                boolean capped = amplifier >= 2;
                if (amplifier < 2 && (guaranteedWither || random.nextFloat() < RADIANT_WITHER_CHANCE)) amplifier++;
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amplifier), source);
                // Only hits against an already capped effect roll the four independent debuffs.
                if (capped) {
                    if (random.nextFloat() < RADIANT_DEBUFF_CHANCE) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100), source);
                    if (random.nextFloat() < RADIANT_DEBUFF_CHANCE) target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100), source);
                    if (random.nextFloat() < RADIANT_DEBUFF_CHANCE) target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200), source);
                    if (random.nextFloat() < RADIANT_DEBUFF_CHANCE) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200), source);
                }
            }
            applyRadiantGlow(target, source);
        } else {
            var poison = PufferfishPoison.of(stack);
            if (poison != null) poison.apply(target, source, duration);
        }
    }

    /// Rolls material poison or applies fixed radiant wither without escalation.
    public static void applyPassiveEffect(ItemStack stack, LivingEntity target, Entity source) {
        if (target instanceof Cat) return;
        if (PufferfishItems.isRadiant(stack)) {
            target.addEffect(passiveEffect(stack), source);
            applyRadiantGlow(target, source);
        } else {
            var poison = PufferfishPoison.of(stack);
            if (poison != null) poison.apply(target, source, STATUS_EFFECT_DURATION_TICKS);
        }
    }

    public static void applyRadiantGlow(LivingEntity target, @javax.annotation.Nullable Entity source) {
        if (target instanceof Cat) return;
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, RADIANT_GLOW_DURATION_TICKS), source);
    }

    /// Returns the material's status before rolling lethal toxin replacement.
    @javax.annotation.Nullable
    public static MobEffectInstance passiveEffect(ItemStack stack) {
        if (PufferfishItems.isRadiant(stack)) return new MobEffectInstance(MobEffects.WITHER, STATUS_EFFECT_DURATION_TICKS, 0);
        @javax.annotation.Nullable var poison = PufferfishPoison.of(stack);
        return poison == null ? null : new MobEffectInstance(MobEffects.POISON, STATUS_EFFECT_DURATION_TICKS, poison.amplifier());
    }

    /// Returns the stored level of the specified enchantment, or zero when absent.
    private static int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.getEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(key))
                .mapToInt(entry -> entry.getIntValue()).findFirst().orElse(0);
    }

    /// Resolves fish and flail damage by material without constructing a temporary fish stack.
    private static float throwingBaseDamage(ItemStack stack) {
        @javax.annotation.Nullable var variant = PufferfishVariant.of(stack);
        var material = variant != null && (stack.getItem() == variant.fishItem() || PufferfishItems.isFlail(stack))
                ? variant.material() : PufferfishMaterial.NORMAL;
        if (material == PufferfishMaterial.HEAVY) return 10;
        var damage = switch (material) {
            case COPPER -> PufferfishCombatConfig.COPPER_DAMAGE;
            case IRON -> PufferfishCombatConfig.IRON_DAMAGE;
            case GOLDEN -> PufferfishCombatConfig.GOLDEN_DAMAGE;
            case DIAMOND -> PufferfishCombatConfig.DIAMOND_DAMAGE;
            case NETHERITE -> PufferfishCombatConfig.NETHERITE_DAMAGE;
            case RADIANT -> PufferfishCombatConfig.RADIANT_DAMAGE;
            default -> PufferfishCombatConfig.NORMAL_DAMAGE;
        };
        return PufferfishCombatConfig.value(damage).floatValue();
    }

    private static float densityBonus(ItemStack stack) {
        int density = enchantmentLevel(stack, Enchantments.DENSITY);
        return density > 0 ? 0.5F + 0.5F * density : 0;
    }

}
