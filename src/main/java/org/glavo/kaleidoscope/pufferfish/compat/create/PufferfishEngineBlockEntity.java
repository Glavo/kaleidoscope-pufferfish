// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.create;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

public final class PufferfishEngineBlockEntity extends GeneratingKineticBlockEntity {
    private ItemStack fish = ItemStack.EMPTY;
    private boolean water;
    private boolean powered;
    private int configuredSpeed;
    private int configuredCapacity;
    private double animationAngle;
    private double previousAnimationAngle;

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide()) {
            previousAnimationAngle = animationAngle;
            animationAngle += getSpeed() * 0.3F;
        }
    }

    public float getAnimationAngle(float partialTick) {
        return (float) ((previousAnimationAngle + (animationAngle - previousAnimationAngle) * partialTick) % 360);
    }

    public PufferfishEngineBlockEntity(BlockPos pos, BlockState state) {
        super(CreateIntegration.ENGINE_ENTITY.get(), pos, state);
        setLazyTickRate(20);
    }

    public ItemStack getFish() {
        return fish.copy();
    }

    public boolean hasWater() {
        return water;
    }

    public boolean isRunning() {
        return water && !fish.isEmpty() && !powered;
    }

    @Override
    public float getGeneratedSpeed() {
        return isRunning() ? configuredSpeed : 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        return lastCapacityProvided = isRunning() && configuredSpeed > 0 ? (float) configuredCapacity / configuredSpeed : 0;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!level.isClientSide()) {
            refreshPower();
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!level.isClientSide()) {
            refreshPower();
        }
    }

    public void refreshPower() {
        boolean nextPowered = level.hasNeighborSignal(worldPosition);
        int nextSpeed = CreateIntegration.SPEED.get();
        int nextCapacity = (PufferfishItems.isHeavy(fish) ? CreateIntegration.HEAVY_CAPACITY
                : PufferfishItems.isNetherite(fish) ? CreateIntegration.NETHERITE_CAPACITY : CreateIntegration.NORMAL_CAPACITY).get();
        if (powered != nextPowered || configuredSpeed != nextSpeed || configuredCapacity != nextCapacity) {
            powered = nextPowered;
            configuredSpeed = nextSpeed;
            configuredCapacity = nextCapacity;
            updateGeneratedRotation();
            setChanged();
        }
    }

    private void contentsChanged() {
        refreshPower();
        updateGeneratedRotation();
        setChanged();
    }

    public ItemInteractionResult useItem(Player player, InteractionHand hand) {
        if (player.isSpectator() || !player.mayBuild()) {
            return ItemInteractionResult.FAIL;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean bucketFish = held.is(Items.PUFFERFISH_BUCKET);
        if (fish.isEmpty() && (bucketFish || PufferfishItems.isPufferfish(held))) {
            if (!level.isClientSide()) {
                fish = bucketFish ? convertFish(held, false) : held.copyWithCount(1);
                if (bucketFish) {
                    water = true;
                    player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.BUCKET)));
                } else if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                contentsChanged();
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!water && held.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide()) {
                water = true;
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.BUCKET)));
                contentsChanged();
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (water && held.is(Items.BUCKET) && (fish.isEmpty() || fish.is(Items.PUFFERFISH))) {
            if (!level.isClientSide()) {
                ItemStack result = fish.isEmpty() ? new ItemStack(Items.WATER_BUCKET) : convertFish(fish, true);
                fish = ItemStack.EMPTY;
                water = false;
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
                contentsChanged();
                level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        // Consume incompatible fish and bucket interactions so they cannot throw or empty into the engine.
        if (bucketFish || PufferfishItems.isPufferfish(held) || held.is(Items.WATER_BUCKET) || held.is(Items.BUCKET)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static ItemStack convertFish(ItemStack source, boolean bucket) {
        ItemStack result = source.transmuteCopy(bucket ? Items.PUFFERFISH_BUCKET : Items.PUFFERFISH, 1);
        Component name = result.get(DataComponents.ITEM_NAME);
        if (name != null && name.getContents() instanceof TranslatableContents translated
                && (translated.getKey().equals("item.kaleidoscope_pufferfish.dyed_pufferfish")
                || translated.getKey().equals("item.kaleidoscope_pufferfish.dyed_pufferfish_bucket"))) {
            result.remove(DataComponents.ITEM_NAME);
        }
        var color = PufferfishColors.get(source);
        if (color != null) {
            PufferfishColors.dye(result, color);
        }
        return result;
    }

    public InteractionResult takeFish(Player player) {
        if (fish.isEmpty() || player.isSpectator() || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack result = fish;
            fish = ItemStack.EMPTY;
            if (!player.addItem(result)) {
                player.drop(result, false);
            }
            contentsChanged();
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public void dropFish() {
        ItemStack result = fish;
        fish = ItemStack.EMPTY;
        Block.popResource(level, worldPosition, result);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        fish = ItemStack.parseOptional(registries, tag.getCompound("Fish"));
        if (!fish.isEmpty() && !PufferfishItems.isPufferfish(fish)) {
            fish = ItemStack.EMPTY;
        }
        if (!fish.isEmpty()) {
            fish.setCount(1);
        }
        water = tag.getBoolean("Water");
        powered = tag.getBoolean("Powered");
        configuredSpeed = tag.getInt("ConfiguredSpeed");
        configuredCapacity = tag.getInt("ConfiguredCapacity");
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!fish.isEmpty()) {
            tag.put("Fish", fish.save(registries));
        }
        tag.putBoolean("Water", water);
        tag.putBoolean("Powered", powered);
        tag.putInt("ConfiguredSpeed", configuredSpeed);
        tag.putInt("ConfiguredCapacity", configuredCapacity);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        String status = fish.isEmpty() ? "empty" : !water ? "dry" : powered ? "stopped" : "running";
        tooltip.add(Component.translatable("gui.kaleidoscope_pufferfish.engine." + status).withStyle(ChatFormatting.GRAY));
        if (!fish.isEmpty()) {
            tooltip.add(fish.getHoverName().copy().withStyle(ChatFormatting.AQUA));
        }
        return true;
    }
}
