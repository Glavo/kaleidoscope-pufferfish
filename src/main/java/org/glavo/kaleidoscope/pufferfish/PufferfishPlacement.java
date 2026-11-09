// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class PufferfishPlacement {
    private PufferfishPlacement() {}

    /// Sneak-use places a fish while retaining normal right-click behavior otherwise.
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var stack = event.getItemStack();
        if (!player.isShiftKeyDown() || player.isSpectator() || !PufferfishItems.isPufferfish(stack)
                || event.getUseItem() == TriState.FALSE) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        var level = event.getLevel();
        var context = new BlockPlaceContext(player, event.getHand(), stack, event.getHitVec());
        var pos = context.getClickedPos();
        var block = PufferfishItems.isHeavy(stack) ? PufferfishBlocks.PLACED_HEAVY_FISH.get() : PufferfishBlocks.PLACED_FISH.get();
        var state = block.getStateForPlacement(context);
        if (!context.canPlace() || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)
                || !state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, CollisionContext.of(player))) return;
        if (!level.isClientSide()) {
            var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
            if (!level.setBlock(pos, state, 2)) return;
            ((PlacedPufferfishBlockEntity) level.getBlockEntity(pos)).setFish(stack);
            if (EventHooks.onBlockPlace(player, snapshot, context.getClickedFace())) {
                level.restoringBlockSnapshots = true;
                try { snapshot.restore(); }
                finally { level.restoringBlockSnapshots = false; }
                return;
            }
            level.updateNeighborsAt(pos, state.getBlock());
            level.playSound(null, pos, SoundEvents.WET_GRASS_PLACE, SoundSource.BLOCKS, 1, 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            stack.consume(1, player);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }
}
