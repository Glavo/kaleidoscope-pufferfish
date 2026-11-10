// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/// Stores the complete inserted fish and preserves the pump item's metadata.
public final class PufferfishPumpBlockEntity extends BlockEntity {
    public static final int REBOUND_TICKS = 8;
    private ItemStack fish = ItemStack.EMPTY;
    private ItemStack pump = PufferfishItems.PUMP.toStack();
    private long lastPress = Long.MIN_VALUE;
    private long animationStart = Long.MIN_VALUE;

    public PufferfishPumpBlockEntity(BlockPos pos, BlockState state) { super(PufferfishBlocks.PUMP_ENTITY.get(), pos, state); }
    public ItemStack fish() { return fish.copy(); }
    public ItemStack pump() { return pump.copy(); }

    public void setFish(ItemStack stack) {
        fish = PufferfishItems.isWeapon(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY;
        changed();
    }

    public void setPump(ItemStack stack) {
        if (stack.is(PufferfishItems.PUMP.get())) pump = stack.copyWithCount(1);
        PufferfishPumpItem.removeDurability(pump);
        changed();
    }

    public ItemStack removeFish() {
        var result = fish;
        fish = ItemStack.EMPTY;
        changed();
        return result;
    }

    public boolean press(Player player) {
        if (!(level instanceof ServerLevel) || player.isSpectator() || pump.isEmpty()) return false;
        long time = level.getGameTime();
        if (lastPress != Long.MIN_VALUE && time - lastPress < REBOUND_TICKS) return false;
        lastPress = time;
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        if (!PufferfishPumpItem.canInflate(fish)) return false;
        fish.setDamageValue(Math.max(0, fish.getDamageValue() - (fish.getMaxDamage() + 9) / 10));
        PufferfishDurability.normalize(fish);
        level.playSound(null, worldPosition, SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.BLOCKS, 0.6F, 1.1F);
        changed();
        return true;
    }

    public float compression(float partialTick) {
        if (level == null || animationStart == Long.MIN_VALUE) return 0;
        return Math.max(0, 1 - (level.getGameTime() - animationStart + partialTick) / REBOUND_TICKS);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1 && level != null) { animationStart = level.getGameTime(); return true; }
        return super.triggerEvent(id, param);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!fish.isEmpty()) tag.put("Fish", fish.save(registries));
        if (!pump.isEmpty()) tag.put("Pump", pump.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        var storedFish = ItemStack.parseOptional(registries, tag.getCompound("Fish"));
        fish = PufferfishItems.isWeapon(storedFish) ? storedFish.copyWithCount(1) : ItemStack.EMPTY;
        var storedPump = ItemStack.parseOptional(registries, tag.getCompound("Pump"));
        pump = storedPump.is(PufferfishItems.PUMP.get()) ? storedPump.copyWithCount(1) : PufferfishItems.PUMP.toStack();
        PufferfishPumpItem.removeDurability(pump);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
