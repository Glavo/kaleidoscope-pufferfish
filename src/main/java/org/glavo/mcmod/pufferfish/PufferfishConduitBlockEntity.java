// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class PufferfishConduitBlockEntity extends ConduitBlockEntity {
    public static final int ANCIENT_RADIUS = 32;
    public PufferfishConduitBlockEntity(BlockPos pos, BlockState state) { super(pos, state); }

    @Override
    public BlockEntityType<?> getType() { return PufferfishBlocks.CONDUIT_ENTITY.get(); }

    public boolean isAncient() { return getBlockState().is(PufferfishBlocks.ANCIENT_CONDUIT.get()); }

    public static boolean isSubmerged(Level level, BlockPos pos) {
        for (var water : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (!level.isWaterAt(water)) return false;
        }
        return true;
    }

    public static void applyPower(Level level, BlockPos pos, int frameSize) {
        boolean ancient = level.getBlockState(pos).is(PufferfishBlocks.ANCIENT_CONDUIT.get());
        int radius = ancient ? ANCIENT_RADIUS : frameSize / 7 * 16;
        var bounds = new AABB(pos).inflate(radius).expandTowards(0, level.getHeight(), 0);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> target instanceof Pufferfish || target instanceof Player && target.isInWaterOrRain())) {
            if (pos.closerThan(target.blockPosition(), radius)) {
                boolean cursed = target instanceof Player && target.hasEffect(PufferfishEffects.PUFFERFISH_CURSE);
                if (cursed) {
                    target.removeEffect(PufferfishEffects.PUFFERFISH_POWER);
                    if (!ancient) continue;
                }
                target.addEffect(new MobEffectInstance(cursed ? PufferfishEffects.PUFFERFISH_CURSE : PufferfishEffects.PUFFERFISH_POWER,
                        260, 0, true, true));
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PufferfishConduitBlockEntity entity) {
        ConduitBlockEntity.serverTick(level, pos, state, entity);
        if (level.getGameTime() % 40 != 0) return;
        int frameSize = 0;
        if (entity.isActive() && !entity.isAncient()) {
            for (var offset : BlockPos.betweenClosed(-2, -2, -2, 2, 2, 2)) {
                int x = Math.abs(offset.getX());
                int y = Math.abs(offset.getY());
                int z = Math.abs(offset.getZ());
                if ((x == 0 && (y == 2 || z == 2)) || (y == 0 && (x == 2 || z == 2))
                        || (z == 0 && (x == 2 || y == 2))) {
                    var framePos = pos.offset(offset);
                    if (level.getBlockState(framePos).isConduitFrame(level, framePos, pos)) frameSize++;
                }
            }
        }
        int radius = entity.isAncient() && entity.isActive() ? ANCIENT_RADIUS : frameSize / 7 * 16;
        PufferfishConduitSpawning.update(level, pos, radius);
    }

    @Override
    public void setRemoved() {
        PufferfishConduitSpawning.update(level, worldPosition, 0);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        PufferfishConduitSpawning.update(level, worldPosition, 0);
        super.onChunkUnloaded();
    }
}
