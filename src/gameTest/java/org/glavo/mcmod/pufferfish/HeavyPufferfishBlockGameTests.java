// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class HeavyPufferfishBlockGameTests {
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void heavyFishFallAndReloadedDisplaysRetainTheirData(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = PufferfishBlocks.PLACED_HEAVY_FISH.get();
        var expected = block.defaultBlockState().setValue(PlacedPufferfishBlock.FACING, Direction.EAST);
        var fish = new ItemStack(PufferfishItems.HEAVY.get());
        fish.set(DataComponents.CUSTOM_NAME, Component.literal("Falling fish"));
        for (int x : new int[] {1, 3}) {
            helper.setBlock(new BlockPos(x, 0, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 5, 2), Blocks.STONE);
            var pos = helper.absolutePos(new BlockPos(x, 6, 2));
            level.setBlockAndUpdate(pos, expected);
            var entity = (PlacedPufferfishBlockEntity) level.getBlockEntity(pos);
            entity.setFish(fish);
            if (x == 3) {
                var saved = entity.saveWithFullMetadata(level.registryAccess());
                var restored = (PlacedPufferfishBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, entity.getBlockState(), saved, level.registryAccess());
                level.setBlockEntity(restored);
                restored.onLoad();
            }
        }
        helper.runAfterDelay(5, () -> {
            for (int x : new int[] {1, 3}) {
                helper.assertBlockPresent(block, new BlockPos(x, 6, 2));
                helper.setBlock(new BlockPos(x, 5, 2), Blocks.AIR);
            }
        });
        helper.runAfterDelay(35, () -> {
            for (int x : new int[] {1, 3}) {
                helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 6, 2));
                var pos = helper.absolutePos(new BlockPos(x, 1, 2));
                var state = level.getBlockState(pos);
                helper.assertTrue(state == expected && state.getLightEmission(level, pos) == 0, "Heavy fish failed to land or became a lamp");
                var entity = (PlacedPufferfishBlockEntity) level.getBlockEntity(pos);
                helper.assertTrue(ItemStack.matches(entity.fish(), fish), "Heavy fish lost components while falling");
                var drops = Block.getDrops(state, level, pos, entity);
                helper.assertTrue(drops.size() == 1 && ItemStack.matches(drops.getFirst(), fish), "Landed heavy fish dropped the wrong item");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void heavyFishDropAnItemWhenTheyCannotLand(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.TORCH);
        helper.setBlock(new BlockPos(2, 6, 2), PufferfishBlocks.PLACED_HEAVY_FISH.get());
        helper.runAfterDelay(35, () -> {
            var pos = helper.absolutePos(new BlockPos(2, 1, 2));
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(PufferfishItems.HEAVY.get())
                    && drops.getFirst().getItem().getCount() == 1, "Falling heavy fish disappeared or duplicated on a blocked landing");
            helper.succeed();
        });
    }
}
