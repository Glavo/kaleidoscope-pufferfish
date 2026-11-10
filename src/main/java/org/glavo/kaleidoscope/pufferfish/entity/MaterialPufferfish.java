// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import org.glavo.kaleidoscope.pufferfish.CopperPufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishCombat;
import org.glavo.kaleidoscope.pufferfish.PufferfishMobs;

/// A swimming material fish whose stored item survives oxidation, capture and release.
public final class MaterialPufferfish extends Pufferfish {
    private static final EntityDataAccessor<ItemStack> FISH = SynchedEntityData.defineId(MaterialPufferfish.class, EntityDataSerializers.ITEM_STACK);

    public MaterialPufferfish(EntityType<? extends MaterialPufferfish> type, Level level, Item item) {
        super(type, level);
        setFish(new ItemStack(item));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FISH, ItemStack.EMPTY);
    }

    public ItemStack fish() { return entityData.get(FISH); }

    public void setFish(ItemStack fish) {
        if (PufferfishMobs.variant(fish) != null) entityData.set(FISH, fish.copyWithCount(1));
    }

    @Override
    protected Component getTypeName() { return fish().getHoverName(); }

    @Override
    public ItemStack getBucketItemStack() { return new ItemStack(PufferfishMobs.variant(fish()).bucket().get()); }

    @Override
    public ItemStack getPickResult() { return getBucketItemStack(); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Fish", fish().save(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadFish(tag);
    }

    public void loadFish(CompoundTag tag) {
        if (tag.contains("Fish")) ItemStack.parse(registryAccess(), tag.getCompound("Fish")).ifPresent(this::setFish);
    }

    @Override
    public void saveToBucketTag(ItemStack stack) {
        super.saveToBucketTag(stack);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, stack, tag -> tag.put("Fish", fish().save(registryAccess())));
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        super.loadFromBucketTag(tag);
        loadFish(tag);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isAlive() && tickCount % 20 == 0 && random.nextInt(1100) == 0) oxidize();
    }

    public void oxidize() {
        int index = CopperPufferfish.index(fish());
        if (index >= 0 && index < 3) setFish(CopperPufferfish.convert(fish(), index + 1));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSpectator() || !player.mayBuild()) return super.mobInteract(player, hand);
        int index = CopperPufferfish.index(fish());
        var tool = player.getItemInHand(hand);
        boolean wax = index >= 0 && index < 4 && tool.is(Items.HONEYCOMB);
        boolean scrape = index >= 4 && tool.canPerformAction(ItemAbilities.AXE_WAX_OFF)
                || index > 0 && index < 4 && tool.canPerformAction(ItemAbilities.AXE_SCRAPE);
        if (!wax && !scrape) return super.mobInteract(player, hand);
        if (!level().isClientSide) {
            setFish(CopperPufferfish.convert(fish(), wax ? index + 4 : index >= 4 ? index - 4 : index - 1));
            setPersistenceRequired();
            level().levelEvent(player, wax ? 3003 : index >= 4 ? 3004 : 3005, blockPosition(), 0);
            level().playSound(null, this, wax ? SoundEvents.HONEYCOMB_WAX_ON : index >= 4 ? SoundEvents.AXE_WAX_OFF : SoundEvents.AXE_SCRAPE,
                    SoundSource.NEUTRAL, 1, 1);
            if (wax) tool.consume(1, player);
            else tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void sting(LivingEntity target) {
        if (!level().isClientSide && getPuffState() > 0 && target.hurt(damageSources().mobAttack(this),
                Math.max(1 + getPuffState(), PufferfishCombat.damage(fish())))) {
            PufferfishCombat.applyPassiveEffect(fish(), target, this);
            playSound(SoundEvents.PUFFER_FISH_STING, 1, 1);
        }
    }

    @Override
    public void playerTouch(Player player) { sting(player); }
}
