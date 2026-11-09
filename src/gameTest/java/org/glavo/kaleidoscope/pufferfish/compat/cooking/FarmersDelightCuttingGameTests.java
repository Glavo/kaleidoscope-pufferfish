// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.PufferfishCuttingGameTests;
import org.glavo.kaleidoscope.pufferfish.PufferfishBlocks;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import vectorwing.farmersdelight.common.registry.ModBlocks;
import vectorwing.farmersdelight.common.registry.ModItems;

/// Exercises Farmer's Delight's actual cutting and output-spawning behavior.
@PrefixGameTestTemplate(false)
public final class FarmersDelightCuttingGameTests {
    /// Prevents construction of the test holder.
    private FarmersDelightCuttingGameTests() {}

    /// Verifies all skin colors, knife requirements, consumption, and empty-board behavior.
    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void farmersDelightCutsFishIntoSashimiAndSkin(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var knife = new ItemStack(ModItems.IRON_KNIFE.get());
        level.setBlockAndUpdate(pos, ModBlocks.CUTTING_BOARD.get().defaultBlockState());
        var board = (CuttingBoardBlockEntity) level.getBlockEntity(pos);
        for (int index = -1; index < 16; index++) {
            var color = index < 0 ? null : DyeColor.byId(index);
            helper.assertTrue(board.addItem(PufferfishCuttingGameTests.fish(color)).isEmpty(), "Board must accept the fish stack");
            helper.assertTrue(!board.processStoredItemUsingTool(new ItemStack(Items.STICK), player), "Stick cut a fish");
            for (int remaining = 1; remaining >= 0; remaining--) {
                helper.assertTrue(board.processStoredItemUsingTool(knife, player)
                        && board.getStoredItem().getCount() == remaining, "Each cut must consume exactly one fish");
                PufferfishCuttingGameTests.verifyDrops(helper, pos, PufferfishBlocks.skin(color));
            }
            helper.assertTrue(!board.processStoredItemUsingTool(knife, player), "Empty board produced repeated output");
        }
        if (ModList.get().isLoaded("kaleidoscope_cookery")) {
            board.addItem(new ItemStack(Items.PUFFERFISH));
            var kitchenKnife = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("kaleidoscope_cookery:iron_kitchen_knife")));
            helper.assertTrue(board.processStoredItemUsingTool(kitchenKnife, player), "Cookery knife could not cut on Farmer's Delight board");
            PufferfishCuttingGameTests.verifyDrops(helper, pos, PufferfishBlocks.skin(null));
        }
        for (var entry : java.util.Map.of(PufferfishItems.GOLDEN.get(), PufferfishBlocks.GOLDEN_SKIN.get(),
                PufferfishItems.NETHERITE.get(), PufferfishBlocks.NETHERITE_SKIN.get(),
                PufferfishItems.HEAVY.get(), PufferfishBlocks.HEAVY_SKIN.get(),
                PufferfishItems.COPPER.get(), PufferfishBlocks.COPPER_SKIN.get(),
                PufferfishItems.IRON.get(), PufferfishBlocks.IRON_SKIN.get(),
                PufferfishItems.DIAMOND.get(), PufferfishBlocks.DIAMOND_SKIN.get()).entrySet()) {
            var special = new ItemStack(entry.getKey(), 2);
            helper.assertTrue(board.addItem(special).isEmpty(), "Board rejected special fish");
            helper.assertTrue(!board.processStoredItemUsingTool(new ItemStack(Items.STICK), player), "Stick cut a special fish");
            for (int remaining = 1; remaining >= 0; remaining--) {
                helper.assertTrue(board.processStoredItemUsingTool(knife, player) && board.getStoredItem().getCount() == remaining,
                        "Special cutting did not consume exactly one fish");
                PufferfishCuttingGameTests.verifyDrops(helper, pos, entry.getValue());
            }
            helper.assertTrue(!board.processStoredItemUsingTool(knife, player), "Special fish produced repeated output");
        }
        for (var item : new net.minecraft.world.item.Item[] {PufferfishItems.SASHIMI.get(), PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get()}) {
            board.addItem(new ItemStack(item));
            helper.assertTrue(!board.processStoredItemUsingTool(knife, player), "Special fish was accepted for cutting");
            board.removeItem();
        }
        helper.succeed();
    }
}
