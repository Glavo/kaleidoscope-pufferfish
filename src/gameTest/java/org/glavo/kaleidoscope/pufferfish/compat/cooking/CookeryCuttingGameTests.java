// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.cooking;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ChoppingBoardBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.PufferfishCuttingGameTests;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishBlocks;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

/// Exercises Cookery's real board, including saved progress and input cancellation.
@PrefixGameTestTemplate(false)
public final class CookeryCuttingGameTests {
    /// Prevents construction of the test holder.
    private CookeryCuttingGameTests() {}

    /// Verifies all colors, knife enforcement, persisted progress, and single-use output collection.
    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void cookeryCutsFishIntoMeatAndSkin(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var knife = new ItemStack(ModItems.IRON_KITCHEN_KNIFE.get());
        level.setBlockAndUpdate(pos, ModBlocks.CHOPPING_BOARD.get().defaultBlockState());
        var board = (ChoppingBoardBlockEntity) level.getBlockEntity(pos);
        for (int index = -1; index < 16; index++) {
            var color = index < 0 ? null : DyeColor.byId(index);
            var fish = PufferfishCuttingGameTests.fish(color);
            helper.assertTrue(board.onPutItem(level, player, fish) && fish.getCount() == 1, "Board must consume one fish");
            helper.assertTrue(!board.onCutItem(level, player, new ItemStack(Items.STICK)), "Stick cut a fish");
            helper.assertTrue(board.onCutItem(level, player, knife), "First knife cut failed");
            var saved = board.saveWithFullMetadata(level.registryAccess());
            level.removeBlockEntity(pos);
            board = new ChoppingBoardBlockEntity(pos, level.getBlockState(pos));
            board.loadWithComponents(saved, level.registryAccess());
            helper.assertTrue(KaleidoscopePufferfish.id(PufferfishBlocks.skinName(color)).equals(board.getModelId()),
                    "Saved cutting progress lost its skin model ID");
            level.setBlockEntity(board);
            for (int cut = 1; cut < 3; cut++) helper.assertTrue(board.onCutItem(level, player, knife), "Knife cut failed");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).isEmpty(), "Products dropped before collection");
            helper.assertTrue(board.onCutItem(level, player, knife), "Output collection failed");
            PufferfishCuttingGameTests.verifyDrops(helper, pos, color);
            helper.assertTrue(!board.onCutItem(level, player, knife), "Empty board produced repeated output");
        }
        for (var item : new net.minecraft.world.item.Item[] {PufferfishItems.GOLDEN.get(), PufferfishItems.NETHERITE.get()}) {
            helper.assertTrue(!board.onPutItem(level, player, new ItemStack(item)), "Special fish was accepted for cutting");
        }
        var fish = PufferfishCuttingGameTests.fish(DyeColor.RED);
        helper.assertTrue(board.onPutItem(level, player, fish) && board.onTakeOut(level, player), "Uncut fish could not be retrieved");
        helper.assertTrue(!board.onCutItem(level, player, knife)
                && level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).isEmpty(), "Cancelled input produced cutting outputs");
        helper.succeed();
    }
}
