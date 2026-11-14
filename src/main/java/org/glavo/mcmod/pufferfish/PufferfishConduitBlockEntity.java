// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class PufferfishConduitBlockEntity extends ConduitBlockEntity {
    public static final TagKey<Block> ANCIENT_FRAME = TagKey.create(Registries.BLOCK,
            PufferfishPlease.id("ancient_conduit_frame"));
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

    public static boolean updateFrame(Level level, BlockPos pos, List<BlockPos> frame) {
        frame.clear();
        if (!isSubmerged(level, pos)) return false;
        for (var offset : BlockPos.betweenClosed(-2, -2, -2, 2, 2, 2)) {
            if (isFrameOffset(offset)) {
                var framePos = pos.offset(offset);
                if (isFrameBlock(level, framePos, pos)) frame.add(framePos);
            }
        }
        return frame.size() >= 16;
    }

    private static boolean isFrameOffset(BlockPos offset) {
        int x = Math.abs(offset.getX()), y = Math.abs(offset.getY()), z = Math.abs(offset.getZ());
        return x == 0 && (y == 2 || z == 2) || y == 0 && (x == 2 || z == 2) || z == 0 && (x == 2 || y == 2);
    }

    private static boolean isFrameBlock(Level level, BlockPos framePos, BlockPos corePos) {
        var state = level.getBlockState(framePos);
        return state.is(ANCIENT_FRAME) || state.isConduitFrame(level, framePos, corePos);
    }

    public static void applyPower(Level level, BlockPos pos, int frameSize) {
        boolean ancient = level.getBlockState(pos).is(PufferfishBlocks.ANCIENT_CONDUIT.get());
        int radius = frameSize / 7 * 16;
        var bounds = new AABB(pos).inflate(radius);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> target instanceof Pufferfish || target instanceof Player)) {
            if (pos.closerThan(target.blockPosition(), radius)) {
                PufferfishCultivation.advanceAtConduit(target);
                if (target instanceof Player && !target.isInWaterOrRain()) continue;
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
