// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/// A single recoverable input; processing happens only through explicit tool use, without an idle ticker.
public final class FishProcessingTableBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    private ResourceLocation cuttingRecipe;
    private int cuts;
    private int brushes;
    private long nextStroke;
    private long animationStart = Long.MIN_VALUE;
    private boolean brushingAnimation;

    public FishProcessingTableBlockEntity(BlockPos pos, BlockState state) {
        super(PufferfishBlocks.PROCESSING_TABLE_ENTITY.get(), pos, state);
    }

    public ItemStack item() { return item.copy(); }

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && (PufferfishItems.isPufferfish(stack) || PufferfishSkinBlock.isSkin(stack)
                || level != null && level.getRecipeManager().getAllRecipesFor(PufferfishPlease.FISH_PROCESSING_TYPE.get())
                        .stream().anyMatch(recipe -> recipe.value().ingredient().test(stack)));
    }

    public boolean insert(ItemStack stack) {
        if (!item.isEmpty() || !accepts(stack)) return false;
        item = stack.copyWithCount(1);
        resetProgress();
        changed();
        return true;
    }

    public ItemStack removeItem() {
        var result = item;
        item = ItemStack.EMPTY;
        resetProgress();
        changed();
        return result;
    }

    private void resetProgress() {
        cuts = brushes = 0;
        cuttingRecipe = null;
        nextStroke = 0;
    }

    public boolean cut(Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server) || player.isSpectator() || !player.mayBuild() || item.isEmpty()) return false;
        var tool = player.getItemInHand(hand);
        var selected = level.getRecipeManager().getRecipeFor(PufferfishPlease.FISH_PROCESSING_TYPE.get(),
                new FishProcessingRecipe.Input(item, tool), level);
        if (selected.isEmpty()) return false;
        if (level.getGameTime() < nextStroke) return true;
        var recipe = selected.get();
        if (!recipe.id().equals(cuttingRecipe)) cuts = 0;
        cuttingRecipe = recipe.id();
        brushes = 0;
        nextStroke = level.getGameTime() + 10;
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        level.playSound(null, worldPosition, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.7F, 1.1F);
        if (++cuts >= recipe.value().strokes()) {
            var outputs = recipe.value().process(item);
            removeItem();
            outputs.forEach(output -> Block.popResource(server, worldPosition.above(), output));
            tool.hurtAndBreak(1, player, slot(hand));
        } else changed();
        return true;
    }

    public boolean canBrush(Player player) {
        if (item.isEmpty() || !(PufferfishItems.isPufferfish(item) || PufferfishSkinBlock.isSkin(item))) return false;
        if (rank().number() > 0) return true;
        if (!PufferfishItems.isPufferfish(item) || !item.has(DataComponents.MAX_DAMAGE)) return false;
        if (PufferfishDurability.canDeactivate(item)) return true;
        explainRestorationFailure(player);
        return false;
    }

    /// Reports the next actionable obstacle without exposing durability implementation details.
    private void explainRestorationFailure(Player player) {
        String reason = item.isDamaged() ? "repair_required"
                : !item.getEnchantments().isEmpty() ? "disenchant_required" : "restore_unavailable";
        player.displayClientMessage(Component.translatable("message.pufferfish_please.processing_table." + reason), true);
    }

    private PufferfishLevel rank() {
        if (PufferfishSkinBlock.isSkin(item)) return item.getOrDefault(PufferfishPlease.SKIN_LEVEL.get(), PufferfishLevel.LEVEL_0);
        var cultivation = item.get(PufferfishPlease.CULTIVATION.get());
        return cultivation == null ? PufferfishLevel.LEVEL_0 : cultivation.level();
    }

    /// Three timed strokes remove one rank; taking the item out discards partial work.
    public boolean brush(Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server) || player.isSpectator() || !player.mayBuild()
                || !(player.getItemInHand(hand).getItem() instanceof BrushItem) || !canBrush(player)) return false;
        long time = level.getGameTime();
        if (time < nextStroke) return true;
        nextStroke = time + 10;
        cuts = 0;
        cuttingRecipe = null;
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 1);
        server.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                worldPosition.getZ() + 0.5, 6, 0.2, 0.12, 0.2, 0.1);
        if (++brushes < 3) return true;
        brushes = 0;
        nextStroke = time + 20;
        var oldRank = rank();
        var newRank = PufferfishLevel.values()[Math.max(0, oldRank.number() - 1)];
        var cultivation = item.get(PufferfishPlease.CULTIVATION.get());
        long refund = !PufferfishSkinBlock.isSkin(item) && cultivation != null ? cultivation.downgradeRefund() : 0;
        player.displayClientMessage(Component.translatable("message.pufferfish_please.processing_table.rank", newRank.displayName()), true);
        if (PufferfishSkinBlock.isSkin(item)) {
            PufferfishSkinBlock.setLevel(item, newRank);
        } else if (newRank != PufferfishLevel.LEVEL_0) {
            item.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(newRank,
                    cultivation.talent(), 0, cultivation.totalExperience()));
        } else {
            item.remove(PufferfishPlease.CULTIVATION.get());
            if (PufferfishDurability.canDeactivate(item)) PufferfishDurability.deactivate(item);
            else if (item.has(DataComponents.MAX_DAMAGE)) {
                explainRestorationFailure(player);
            }
        }
        player.getItemInHand(hand).hurtAndBreak(1, player, slot(hand));
        server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 0.8F);
        changed();
        dropExperience(server, refund);
        return true;
    }

    private void dropExperience(ServerLevel server, long amount) {
        while (amount > 0) {
            // Vanilla saves and synchronizes each orb's value as a short, with an int count for merged orbs.
            int value = (int) Math.min(Short.MAX_VALUE, amount);
            int count = (int) Math.min(Integer.MAX_VALUE, amount / value);
            var orb = new ExperienceOrb(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                    worldPosition.getZ() + 0.5, value);
            if (count > 1) {
                var data = new CompoundTag();
                orb.addAdditionalSaveData(data);
                data.putInt("Count", count);
                orb.readAdditionalSaveData(data);
            }
            server.addFreshEntity(orb);
            amount -= (long) value * count;
        }
    }

    /// Uses the vanilla brush cadence and targeting; other blocks retain their normal brushing behavior.
    public static void brushTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || !(event.getItem().getItem() instanceof BrushItem) || player.isSpectator() || !player.mayBuild()) return;
        int elapsed = event.getItem().getUseDuration(player) - event.getDuration() + 1;
        if (elapsed % 10 != 5) return;
        var hit = ProjectileUtil.getHitResultOnViewVector(player,
                entity -> !entity.isSpectator() && entity.isPickable(), player.blockInteractionRange());
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                && player.level().mayInteract(player, block.getBlockPos())
                && player.mayUseItemAt(block.getBlockPos(), block.getDirection(), event.getItem())
                && player.level().getBlockEntity(block.getBlockPos()) instanceof FishProcessingTableBlockEntity table) {
            if (!table.brush(player, player.getUsedItemHand())) player.stopUsingItem();
            else if (event.getDuration() < 20) event.setDuration(event.getDuration() + 100);
        }
    }

    private static EquipmentSlot slot(InteractionHand hand) { return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND; }

    public float motion(float partialTick) {
        return level == null || animationStart == Long.MIN_VALUE ? 0
                : Math.max(0, 1 - (level.getGameTime() - animationStart + partialTick) / 8F);
    }
    public boolean isBrushingAnimation() { return brushingAnimation; }

    @Override public boolean triggerEvent(int id, int param) {
        if (id == 1 && level != null) {
            animationStart = level.getGameTime();
            brushingAnimation = param == 1;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Empty update packets are ignored by NeoForge; explicitly synchronize the empty slot.
        tag.put("Item", item.saveOptional(registries));
        if (cuttingRecipe != null) {
            tag.putString("CuttingRecipe", cuttingRecipe.toString());
            tag.putInt("Cuts", cuts);
        }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        item = ItemStack.parseOptional(registries, tag.getCompound("Item"));
        resetProgress();
        cuttingRecipe = ResourceLocation.tryParse(tag.getString("CuttingRecipe"));
        cuts = Math.clamp(tag.getInt("Cuts"), 0, 63);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
