// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.chinesefood;

import com.bmt.kaleidoscope_chinesefood.block.FreezerBlock;
import com.bmt.kaleidoscope_chinesefood.block.entity.FreezerBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.StockpotInput;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishBlocks;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.glavo.kaleidoscope.pufferfish.PufferfishLamps;
import org.glavo.kaleidoscope.pufferfish.compat.cookery.CookeryFoodIntegration;

@PrefixGameTestTemplate(false)
public final class PufferfishAspicGameTests {
    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void brothRequiresTwoOrdinaryOrDyedSkinsAndWater(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = (StockpotRecipe) level.getRecipeManager().byKey(KaleidoscopePufferfish.id("compat/cookery/pufferfish_skin_broth")).orElseThrow().value();
        helper.assertTrue(recipe.time() == 1200 && recipe.carrier().test(new ItemStack(Items.BOWL)), "Incorrect broth cooking time or carrier");
        var ingredients = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int first = -1; first < 16; first++) {
            for (int second = -1; second < 16; second++) {
                ingredients.set(0, new ItemStack(PufferfishBlocks.skin(first < 0 ? null : DyeColor.byId(first))));
                ingredients.set(1, new ItemStack(PufferfishBlocks.skin(second < 0 ? null : DyeColor.byId(second))));
                var input = new StockpotInput(ingredients, ResourceLocation.withDefaultNamespace("water"));
                var selected = level.getRecipeManager().getRecipeFor(ModRecipes.STOCKPOT_RECIPE, input, level).orElseThrow().value();
                var result = selected.assemble(input, level.registryAccess());
                helper.assertTrue(result.is(PufferfishItems.SKIN_BROTH.get()) && result.getCount() == 1, "Mixed skins matched the wrong soup");
                helper.assertTrue(!recipe.matches(new StockpotInput(ingredients, ResourceLocation.withDefaultNamespace("milk")), level), "Broth accepted another soup base");
            }
        }
        for (var variant : PufferfishLamps.VARIANTS.values()) {
            var fish = variant.fish().get();
            if (fish.is(Items.PUFFERFISH)) continue;
            ingredients.set(0, new ItemStack(PufferfishBlocks.skinForFish(fish)));
            helper.assertTrue(!recipe.matches(new StockpotInput(ingredients, recipe.soupBase()), level), "Broth accepted a metallic or waxed skin");
        }
        ingredients.set(0, new ItemStack(PufferfishBlocks.skin(null)));
        ingredients.set(1, ItemStack.EMPTY);
        helper.assertTrue(!recipe.matches(new StockpotInput(ingredients, recipe.soupBase()), level), "One skin produced concentrated broth");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish", timeoutTicks = 1260)
    public static void stockpotCooksOneBowlOfBrothInSixtySeconds(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        level.setBlockAndUpdate(pos, ModBlocks.STOCKPOT.get().defaultBlockState());
        var pot = (StockpotBlockEntity) level.getBlockEntity(pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(pot.addSoupBase(level, player, new ItemStack(Items.WATER_BUCKET)), "Stockpot rejected water");
        for (var color : new DyeColor[] {null, DyeColor.BLUE}) {
            helper.assertTrue(pot.addIngredient(level, player, new ItemStack(PufferfishBlocks.skin(color))), "Stockpot rejected a skin");
        }
        helper.assertTrue(pot.onLitClick(level, player, new ItemStack(com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems.STOCKPOT_LID.get())),
                "Stockpot lid placement failed");
        helper.runAtTickTime(1190, () -> helper.assertTrue(pot.getStatus() != StockpotBlockEntity.FINISHED, "Broth cooked before sixty seconds"));
        helper.runAtTickTime(1230, () -> {
            helper.assertTrue(pot.getTakeoutCount() == 1 && pot.getStatus() == StockpotBlockEntity.FINISHED, "Stockpot did not finish one portion");
            pot.onLitClick(level, player, ItemStack.EMPTY);
            var bowls = new ItemStack(Items.BOWL, 2);
            helper.assertTrue(pot.takeOutProduct(level, player, bowls) && bowls.getCount() == 1
                    && player.getInventory().countItem(PufferfishItems.SKIN_BROTH.get()) == 1, "Broth did not consume exactly one bowl");
            helper.assertTrue(!pot.takeOutProduct(level, player, bowls), "Stockpot duplicated broth");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void refrigeratorSetsBrothAndPreservesStacksQualityAndSavedProgress(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var block = com.bmt.kaleidoscope_chinesefood.init.ModBlocks.FREEZER.get();
        var recipe = (PufferfishAspicRecipe) level.getRecipeManager().byKey(KaleidoscopePufferfish.id("compat/chinesefood/pufferfish_aspic")).orElseThrow().value();
        helper.assertTrue(recipe.calculateProcessingTime(1) == 2400, "Incorrect chilling time");
        for (boolean top : new boolean[] {true, false}) {
            for (int count : new int[] {1, 4, 16}) {
                var state = block.defaultBlockState().setValue(FreezerBlock.TOP, top);
                var freezer = new FreezerBlockEntity(pos, state);
                freezer.setLevel(level);
                freezer.onLoad();
                var broth = new ItemStack(PufferfishItems.SKIN_BROTH.get(), count);
                QualityUtils.setQuality(broth, Quality.SUPERB);
                freezer.setItem(0, broth);
                for (int tick = 0; tick < 1200; tick++) FreezerBlockEntity.serverTick(level, pos, state, freezer);
                helper.assertTrue(freezer.getItem(0).is(PufferfishItems.SKIN_BROTH.get()), "Broth set too early");
                var saved = freezer.saveWithFullMetadata(level.registryAccess());
                freezer = new FreezerBlockEntity(pos, state);
                freezer.setLevel(level);
                freezer.loadWithComponents(saved, level.registryAccess());
                freezer.onLoad();
                for (int tick = 1200; tick <= recipe.calculateProcessingTime(count); tick++) FreezerBlockEntity.serverTick(level, pos, state, freezer);
                var result = freezer.getItem(0);
                helper.assertTrue(result.is(top ? CookeryFoodIntegration.ASPIC_ITEM.get() : PufferfishItems.SKIN_BROTH.get())
                        && result.getCount() == count && QualityUtils.getQuality(result) == Quality.SUPERB,
                        "Refrigerator lost progress, quantity, quality, or used the wrong compartment");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void aspicProvidesFourPortionsAndReturnsOneBowl(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = CookeryFoodIntegration.ASPIC.get();
        var item = CookeryFoodIntegration.ASPIC_ITEM.get();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int count : new int[] {1, 2}) {
            player.getInventory().clearContent();
            player.getFoodData().setFoodLevel(0);
            player.removeAllEffects();
            var remaining = new ItemStack(item, count).finishUsingItem(level, player);
            helper.assertTrue(player.getFoodData().getFoodLevel() == 8 && player.hasEffect(MobEffects.POISON), "Aspic nutrition or poison missing");
            helper.assertTrue(count == 1 ? remaining.is(Items.BOWL) : remaining.is(item) && remaining.getCount() == 1
                    && player.getInventory().countItem(Items.BOWL) == 1, "Eating aspic lost or duplicated the bowl");
        }
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        for (int bite = 0; bite < 4; bite++) {
            player.getFoodData().setFoodLevel(0);
            var state = level.getBlockState(pos);
            var drops = Block.getDrops(state, level, pos, null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(bite == 0 ? item : Items.BOWL), "Incorrect partial meal drop");
            state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            helper.assertTrue(player.getFoodData().getFoodLevel() == 2 && level.getBlockState(pos).getValue(block.getBites()) == bite + 1,
                    "Aspic portion count or nutrition is incorrect");
        }
        var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(Items.BOWL), "Empty aspic bowl duplicated food");
        helper.succeed();
    }
}
