// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/// Stores one complete fish stack for rendering and lossless recovery.
public final class PlacedPufferfishBlockEntity extends BlockEntity {
    private ItemStack fish = ItemStack.EMPTY;

    public PlacedPufferfishBlockEntity(BlockPos pos, BlockState state) {
        super(PufferfishBlocks.PLACED_FISH_ENTITY.get(), pos, state);
    }

    public ItemStack fish() { return fish.copy(); }

    public void setFish(ItemStack stack) {
        fish = PufferfishItems.isPufferfish(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        scheduleHeavyConversion();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        scheduleHeavyConversion();
    }

    private void scheduleHeavyConversion() {
        if (level != null && !level.isClientSide && getBlockState().is(PufferfishBlocks.PLACED_FISH.get()) && PufferfishItems.isHeavy(fish)) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!fish.isEmpty()) tag.put("Fish", fish.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        var stack = ItemStack.parseOptional(registries, tag.getCompound("Fish"));
        fish = PufferfishItems.isPufferfish(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
