// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.cooking;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.ChoppingBoardBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.IStockpot;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.StockpotInput;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.FlexStockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
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

    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish", timeoutTicks = 380)
    public static void stockpotCooksSashimiAndLettuceIntoPufferfishSoup(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int batch = 0; batch < 2; batch++) {
            var relative = new BlockPos(1 + batch * 2, 2, 1);
            helper.setBlock(relative.below(), Blocks.CAMPFIRE);
            helper.setBlock(relative, ModBlocks.STOCKPOT.get());
            var pot = (StockpotBlockEntity) level.getBlockEntity(helper.absolutePos(relative));
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var water = new ItemStack(Items.WATER_BUCKET);
            helper.assertTrue(pot.addSoupBase(level, player, water) && water.isEmpty(), "Stockpot rejected its water base");
            int fishCount = batch == 0 ? 1 : 2;
            int lettuceCount = batch == 0 ? 1 : 3;
            for (int i = 0; i < fishCount + lettuceCount; i++) {
                var ingredient = new ItemStack(i < fishCount ? PufferfishItems.SASHIMI.get() : ModItems.LETTUCE.get());
                helper.assertTrue(pot.addIngredient(level, player, ingredient) && ingredient.isEmpty(), "Stockpot did not consume soup ingredients");
            }
            helper.assertTrue(pot.onLitClick(level, player, new ItemStack(ModItems.STOCKPOT_LID.get())), "Stockpot lid could not be closed");
            helper.runAtTickTime(10, () -> helper.assertTrue(pot.getStatus() == IStockpot.COOKING
                    && pot.getResult().is(ModItems.PUFFERFISH_SOUP.get()), "Sashimi and lettuce did not start cooking pufferfish soup"));
            helper.runAtTickTime(340, () -> {
                helper.assertTrue(pot.getStatus() == IStockpot.FINISHED, "Pufferfish soup did not finish cooking");
                helper.assertTrue(pot.onLitClick(level, player, ItemStack.EMPTY), "Finished stockpot could not be opened");
                var bowl = new ItemStack(Items.BOWL);
                helper.assertTrue(pot.takeOutProduct(level, player, bowl) && bowl.isEmpty()
                        && player.getInventory().countItem(ModItems.PUFFERFISH_SOUP.get()) == 1, "Stockpot did not serve a bowl of pufferfish soup");
                helper.assertTrue(!pot.takeOutProduct(level, player, new ItemStack(Items.BOWL)), "Empty stockpot produced duplicate soup");
            });
        }
        helper.runAtTickTime(345, helper::succeed);
    }

    /// Verifies whole fish, slices, and mixed batches in Cookery's strict and flexible soup recipes.
    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void cookerySoupAcceptsWholeFishAndSashimi(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var strict = (StockpotRecipe) manager.byKey(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "stockpot/pufferfish_soup"))
                .orElseThrow().value();
        var flex = (FlexStockpotRecipe) manager.byKey(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "flex_stockpot/pufferfish_soup"))
                .orElseThrow().value();
        var sashimiStrict = (StockpotRecipe) manager.byKey(KaleidoscopePufferfish.id("compat/cookery/pufferfish_soup_from_sashimi"))
                .orElseThrow().value();
        var sashimiFlex = (FlexStockpotRecipe) manager.byKey(KaleidoscopePufferfish.id("compat/cookery/pufferfish_soup_from_sashimi_flexible"))
                .orElseThrow().value();
        for (int index = -1; index < 16; index++) {
            var whole = PufferfishCuttingGameTests.fish(index < 0 ? null : DyeColor.byId(index)).copyWithCount(1);
            var sashimi = new ItemStack(PufferfishItems.SASHIMI.get());
            for (int mode = 0; mode < 3; mode++) {
                var stacks = NonNullList.withSize(StockpotRecipe.RECIPES_SIZE, ItemStack.EMPTY);
                stacks.set(0, mode == 0 ? sashimi : whole);
                stacks.set(1, mode == 2 ? whole : sashimi);
                for (int i = 2; i < 5; i++) stacks.set(i, new ItemStack(ModItems.LETTUCE.get()));
                var input = new StockpotInput(stacks, strict.soupBase());
                helper.assertTrue(strict.matches(input, level) == (mode == 2) && flex.matches(input, level) == (mode == 2),
                        "Original soup recipes were overridden or rejected whole fish");
                helper.assertTrue(sashimiStrict.matches(input, level) == (mode != 2) && sashimiFlex.matches(input, level) == (mode == 0),
                        "Sashimi soup recipes overlapped with whole-fish recipes");
                var selected = manager.getRecipeFor(ModRecipes.STOCKPOT_RECIPE, input, level).orElseThrow();
                helper.assertTrue(selected.value().getResultItem(level.registryAccess()).is(ModItems.PUFFERFISH_SOUP.get())
                        && selected.id().getNamespace().equals(mode == 2 ? "kaleidoscope_cookery" : KaleidoscopePufferfish.MODID),
                        "Strict soup resolved to the wrong dish or namespace");
                var flexible = manager.getRecipeFor(ModRecipes.FLEX_STOCKPOT_RECIPE, input, level).orElseThrow().value();
                helper.assertTrue(flexible.getResultItem(level.registryAccess()).is(ModItems.PUFFERFISH_SOUP.get()), "Flexible soup rejected or misclassified a mixed batch");
                if (mode == 2) for (String name : java.util.List.of("mixed_pufferfish_soup", "mixed_pufferfish_soup_three_types")) {
                    var mixed = manager.byKey(KaleidoscopePufferfish.id("compat/cookery/" + name)).orElseThrow().value();
                    helper.assertTrue(!((FlexStockpotRecipe) mixed).matches(input, level), "Mixed soup matched without sashimi");
                }
            }
        }
        helper.succeed();
    }

    /// Verifies all colors, knife enforcement, persisted progress, and single-use output collection.
    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void cookeryCutsFishIntoSashimiAndSkin(GameTestHelper helper) {
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
            PufferfishCuttingGameTests.verifyDrops(helper, pos, PufferfishBlocks.skin(color));
            helper.assertTrue(!board.onCutItem(level, player, knife), "Empty board produced repeated output");
        }
        for (var entry : java.util.Map.of(PufferfishItems.GOLDEN.get(), PufferfishBlocks.GOLDEN_SKIN.get(),
                PufferfishItems.NETHERITE.get(), PufferfishBlocks.NETHERITE_SKIN.get(),
                PufferfishItems.HEAVY.get(), PufferfishBlocks.HEAVY_SKIN.get()).entrySet()) {
            var special = new ItemStack(entry.getKey(), 2);
            special.set(KaleidoscopePufferfish.COLOR.get(), DyeColor.RED);
            helper.assertTrue(board.onPutItem(level, player, special) && special.getCount() == 1, "Board did not consume one special fish");
            helper.assertTrue(!board.onCutItem(level, player, new ItemStack(Items.STICK)) && board.onCutItem(level, player, knife), "Special fish knife requirement failed");
            var saved = board.saveWithFullMetadata(level.registryAccess());
            level.removeBlockEntity(pos);
            board = new ChoppingBoardBlockEntity(pos, level.getBlockState(pos));
            board.loadWithComponents(saved, level.registryAccess());
            level.setBlockEntity(board);
            helper.assertTrue((entry.getKey() == PufferfishItems.GOLDEN.get() ? KaleidoscopePufferfish.id("golden_pufferfish")
                        : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(entry.getValue())).equals(board.getModelId()), "Special cutting progress lost its model");
            for (int cut = 1; cut < 3; cut++) helper.assertTrue(board.onCutItem(level, player, knife), "Special fish cutting failed");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).isEmpty(), "Special products dropped before collection");
            helper.assertTrue(board.onCutItem(level, player, knife), "Special output collection failed");
            PufferfishCuttingGameTests.verifyDrops(helper, pos, entry.getValue());
            helper.assertTrue(!board.onCutItem(level, player, knife), "Special fish produced repeated output");
        }
        for (var item : new net.minecraft.world.item.Item[] {PufferfishItems.SASHIMI.get(), PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get()}) {
            helper.assertTrue(!board.onPutItem(level, player, new ItemStack(item)), "Special fish was accepted for cutting");
        }
        var fish = PufferfishCuttingGameTests.fish(DyeColor.RED);
        helper.assertTrue(board.onPutItem(level, player, fish) && board.onTakeOut(level, player), "Uncut fish could not be retrieved");
        helper.assertTrue(!board.onCutItem(level, player, knife)
                && level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).isEmpty(), "Cancelled input produced cutting outputs");
        helper.succeed();
    }
}
