// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class PufferfishContactDamage {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, KaleidoscopePufferfish.id("pufferfish_prick"));

    private PufferfishContactDamage() {}

    /// Deals at most one point per second, including contact with walls and ceilings.
    public static void tick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide || !entity.isAlive() || entity.isSpectator() || entity.tickCount % 20 != 0) return;
        var level = entity.level();
        var bounds = entity.getBoundingBox().inflate(0.001);
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PufferfishSkinBlock) && !(state.getBlock() instanceof PlacedPufferfishBlock)) continue;
            for (var shape : state.getCollisionShape(level, pos, CollisionContext.of(entity)).toAabbs()) {
                if (shape.move(pos).intersects(bounds)) {
                    entity.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE)), 1);
                    return;
                }
            }
        }
    }
}
