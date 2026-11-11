// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/// Applies Channeling to fish projectiles without changing vanilla's trident-only predicates.
public final class PufferfishChanneling {
    public static final String LIGHTNING_MARKER = "pufferfish_please:channeling";
    private PufferfishChanneling() {}

    /// Called only after a successful projectile hit; Endermen follow the trident exemption.
    public static void hitEntity(Projectile projectile, ItemStack stack, Entity target) {
        if (target.getType() != EntityType.ENDERMAN) strike(projectile, stack, target.position());
    }

    public static void hitBlock(Projectile projectile, ItemStack stack, BlockHitResult hit) {
        if (projectile.level().getBlockState(hit.getBlockPos()).is(Blocks.LIGHTNING_ROD)) {
            strike(projectile, stack, hit.getBlockPos().clampLocationWithin(hit.getLocation()));
        }
    }

    private static void strike(Projectile projectile, ItemStack stack, Vec3 position) {
        if (!(projectile.level() instanceof ServerLevel level) || !level.isThundering()
                || !level.canSeeSky(BlockPos.containing(position))) return;
        var enchantment = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.CHANNELING);
        if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) <= 0) return;
        var lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning == null) return;
        lightning.moveTo(position);
        lightning.getPersistentData().putBoolean(LIGHTNING_MARKER, true);
        if (projectile.getOwner() instanceof ServerPlayer player) lightning.setCause(player);
        if (level.addFreshEntity(lightning)) {
            level.playSound(null, position.x, position.y, position.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 5, 1);
        }
    }
}
