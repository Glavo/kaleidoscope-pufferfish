// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.glavo.kaleidoscope.pufferfish.entity.ThrownPufferfish;

/// Handles pufferfish throwing with item-specific speeds reduced by Density and normal item consumption.
public final class PufferfishThrowing {
    /// Prevents construction of this interaction handler.
    private PufferfishThrowing() {}

    /// Handles eligible right-clicks, spawning a fish on the server and consuming one item on success.
    public static void use(PlayerInteractEvent.RightClickItem event) {
        var player = event.getEntity();
        var stack = event.getItemStack();
        if (!PufferfishItems.isPufferfish(stack) || player.isShiftKeyDown() || player.isSpectator()
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        var level = event.getLevel();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if (!level.isClientSide()) {
            var projectile = new ThrownPufferfish(level, player);
            projectile.setItem(stack);
            float speed = PufferfishCombatConfig.value(PufferfishItems.isHeavy(stack) ? PufferfishCombatConfig.HEAVY_THROW_SPEED : PufferfishItems.isNetherite(stack)
                    ? PufferfishCombatConfig.NETHERITE_THROW_SPEED : PufferfishCombatConfig.NORMAL_THROW_SPEED).floatValue();
            int density = stack.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY));
            speed /= 1 + PufferfishCombatConfig.value(PufferfishCombatConfig.DENSITY_THROW_SPEED_PENALTY) * density;
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, 1.0F);
            if (!level.addFreshEntity(projectile)) {
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                    SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        }
        stack.consume(1, player);
    }
}
