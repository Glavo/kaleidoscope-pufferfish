// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;

/// A single direct hit can awaken its source fish; no progress is stored before success.
public final class PufferfishAwakening {
    private static final ThreadLocal<Hit> CURRENT_HIT = new ThreadLocal<>();

    private PufferfishAwakening() {}

    private record Recipient(Entity owner, Supplier<ItemStack> stack, Consumer<ItemStack> save) {}

    private static final class Hit {
        final LivingEntity target;
        final DamageSource source;
        boolean effective;
        LivingDeathEvent death;

        Hit(LivingEntity target, DamageSource source) { this.target = target; this.source = source; }
        boolean matches(LivingEntity target, DamageSource source) { return this.target == target && this.source == source; }
    }

    private static boolean eligible(ItemStack stack) {
        return stack.getCount() == 1 && PufferfishItems.isWeapon(stack) && !stack.has(PufferfishPlease.CULTIVATION.get());
    }

    /// Resolves copying projectiles and item-specific thorns before their caller recovers or replaces the item.
    public static boolean track(ItemStack stack, Entity owner, LivingEntity target, DamageSource source, BooleanSupplier damage) {
        if (!(target.level() instanceof ServerLevel) || !eligible(stack)) return damage.getAsBoolean();
        var previous = CURRENT_HIT.get();
        var hit = new Hit(target, source);
        CURRENT_HIT.set(hit);
        boolean result;
        try {
            result = damage.getAsBoolean();
        } finally {
            if (previous == null) CURRENT_HIT.remove();
            else CURRENT_HIT.set(previous);
        }
        if (hit.effective) {
            awaken(new Recipient(owner, () -> stack, ignored -> {}), target,
                    hit.death != null && !hit.death.isCanceled() && target.isDeadOrDying());
        }
        return result;
    }

    public static void afterDamage(LivingDamageEvent.Post event) {
        var target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        var hit = CURRENT_HIT.get();
        if (hit != null && hit.matches(target, event.getSource())) {
            hit.effective = true;
            return;
        }
        var recipient = recipient(event.getSource());
        if (recipient == null) return;
        if (!target.isDeadOrDying()) {
            level.getServer().tell(new TickTask(level.getServer().getTickCount(), () -> awaken(recipient, target, false)));
        }
        else {
            // A totem or canceled death that restores health still counts as one nonlethal hit.
            level.getServer().tell(new TickTask(level.getServer().getTickCount(), () -> {
                if (!target.isDeadOrDying()) awaken(recipient, target, false);
            }));
        }
    }

    public static void afterDeath(LivingDeathEvent event) {
        var target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) return;
        var hit = CURRENT_HIT.get();
        if (hit != null && hit.matches(target, event.getSource())) {
            hit.death = event;
            return;
        }
        var recipient = recipient(event.getSource());
        if (recipient == null) return;
        level.getServer().tell(new TickTask(level.getServer().getTickCount(), () -> {
            if (!event.isCanceled() && target.isDeadOrDying()) awaken(recipient, target, true);
        }));
    }

    private static Recipient recipient(DamageSource source) {
        if (source.getEntity() != source.getDirectEntity()) return null;
        if (source.getEntity() instanceof Pufferfish fish
                && (source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.THORNS))) {
            if (fish instanceof MaterialPufferfish material) {
                return eligible(material.fish()) ? new Recipient(fish, () -> material.fish().copy(), material::setFish) : null;
            }
            var stored = fish.getExistingDataOrNull(PufferfishPlease.FISH_ITEM);
            if (stored != null && !stored.isEmpty() && !eligible(stored)) return null;
            return new Recipient(fish, () -> {
                var current = fish.getExistingDataOrNull(PufferfishPlease.FISH_ITEM);
                var item = current == null || current.isEmpty() ? new ItemStack(Items.PUFFERFISH) : current.copy();
                var color = PufferfishColors.get(fish);
                return color == null ? item : PufferfishColors.dye(item, color);
            }, item -> fish.setData(PufferfishPlease.FISH_ITEM, item));
        }
        if ((source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))
                && source.getEntity() instanceof LivingEntity attacker) {
            var stack = attacker.getMainHandItem();
            if (eligible(stack)) return new Recipient(attacker, () -> stack, ignored -> {});
        }
        return null;
    }

    private static void awaken(Recipient recipient, LivingEntity target, boolean killed) {
        var stack = recipient.stack().get();
        if (!eligible(stack) || !(target instanceof Mob || target instanceof net.minecraft.world.entity.player.Player)) return;
        double chance = killed && target instanceof Mob ? target instanceof Enemy ? 0.5 : 0.1 : 0.05;
        var random = recipient.owner().getRandom();
        if (random.nextDouble() >= chance) return;
        PufferfishDurability.activate(stack);
        var cultivation = new PufferfishCultivation(PufferfishLevel.LEVEL_1, random.nextDouble(), 0, 0);
        stack.set(PufferfishPlease.CULTIVATION.get(), cultivation);
        recipient.save().accept(stack);
        var level = (ServerLevel) target.level();
        var owner = recipient.owner();
        level.sendParticles(ParticleTypes.ENCHANT, owner.getX(), owner.getY() + owner.getBbHeight() * 0.5, owner.getZ(), 12, 0.3, 0.3, 0.3, 0);
        level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.5F, 1.5F);
        if (owner instanceof ServerPlayer player) PufferfishCultivation.announcePromotion(player, stack, PufferfishLevel.LEVEL_0);
    }
}
