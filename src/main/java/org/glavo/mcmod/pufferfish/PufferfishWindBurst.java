// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.Vec3;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;

/// Adds a nondamaging, fixed-radius gust to an enchanted fish's outward impact.
public final class PufferfishWindBurst {
    private PufferfishWindBurst() {}

    public static void burst(ThrownPufferfish fish, Vec3 position) {
        if (!(fish.level() instanceof ServerLevel level)) return;
        var enchantment = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.WIND_BURST);
        int rank = EnchantmentHelper.getItemEnchantmentLevel(enchantment, fish.getItem());
        if (rank <= 0) return;
        float strength = switch (rank) {
            case 1 -> 1.00F;
            case 2 -> 1.22F;
            default -> 1.40F;
        };
        var calculator = new SimpleExplosionDamageCalculator(false, false, Optional.of(strength), Optional.empty());
        var explosion = level.explode(fish, null, calculator, position.x, position.y, position.z, 1.2F, false,
                Level.ExplosionInteraction.NONE, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE,
                SoundEvents.WIND_CHARGE_BURST);
        // Keep native landing-height accounting while retaining the fish as the explosion source.
        explosion.getHitPlayers().forEach((player, impulse) -> {
            if (player instanceof ServerPlayer && impulse.lengthSqr() > 0) {
                player.setIgnoreFallDamageFromCurrentImpulse(true);
            }
        });
    }
}
