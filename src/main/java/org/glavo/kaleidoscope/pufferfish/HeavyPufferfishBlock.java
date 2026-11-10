// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class HeavyPufferfishBlock extends PlacedPufferfishBlock implements Fallable {
    public static final MapCodec<HeavyPufferfishBlock> CODEC = simpleCodec(HeavyPufferfishBlock::new);

    public HeavyPufferfishBlock(Properties properties) { super(properties); }

    @Override
    protected MapCodec<HeavyPufferfishBlock> codec() { return CODEC; }

    @Override
    public Item asItem() { return PufferfishLamps.isLamp(this) ? super.asItem() : PufferfishItems.HEAVY.get(); }

    @Override
    public PlacedPufferfishBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var entity = (PlacedPufferfishBlockEntity) super.newBlockEntity(pos, state);
        entity.setFish(new ItemStack(PufferfishItems.HEAVY.get()));
        return entity;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        level.scheduleTick(pos, this, 2);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        level.scheduleTick(pos, this, 2);
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!FallingBlock.isFree(level.getBlockState(pos.below())) || pos.getY() < level.getMinBuildHeight()) return;
        var blockEntity = level.getBlockEntity(pos);
        var data = blockEntity == null ? null : blockEntity.saveWithoutMetadata(level.registryAccess());
        var falling = FallingBlockEntity.fall(level, pos, state);
        falling.blockData = data;
        falling.setHurtsEntities(2.0F, 40);
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState replaced, FallingBlockEntity falling) {
        if (!falling.isSilent()) level.levelEvent(1031, pos, 0);
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return new DamageSource(entity.damageSources().anvil(entity).typeHolder(), entity) {
            @Override
            public Component getLocalizedDeathMessage(LivingEntity victim) {
                return Component.translatable("death.attack.kaleidoscope_pufferfish.falling_pufferfish", victim.getDisplayName());
            }
        };
    }

    public static void poisonOnImpact(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide || !event.getSource().is(DamageTypes.FALLING_ANVIL)
                || !(event.getSource().getDirectEntity() instanceof FallingBlockEntity falling)
                || !(falling.getBlockState().getBlock() instanceof HeavyPufferfishBlock)
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        PufferfishCombat.applyPassiveEffect(new ItemStack(PufferfishItems.HEAVY.get()), event.getEntity(), falling);
    }
}
