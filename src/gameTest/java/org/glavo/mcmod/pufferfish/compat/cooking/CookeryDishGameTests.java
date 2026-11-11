// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.compat.cooking;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.SimpleInput;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.StockpotInput;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.PotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import org.glavo.mcmod.pufferfish.test.CuttingAssertions;

/// Exercises stir-frying, rice soup, and Cookery's rice bowl crafting and container behavior.
@PrefixGameTestTemplate(false)
public final class CookeryDishGameTests {
    private CookeryDishGameTests() {}

    @GameTest(template = "empty", templateNamespace = "pufferfish_please")
    public static void baoziUsesVegetableAndDoughTagsWithoutRecipeConflicts(GameTestHelper helper) {
        var level = helper.getLevel();
        var vegetables = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ResourceLocation.parse("c:vegetables"));
        var dough = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ResourceLocation.parse("c:dough"));
        int checked = 0;
        for (var vegetable : BuiltInRegistries.ITEM.getTagOrEmpty(vegetables)) {
            for (var flour : BuiltInRegistries.ITEM.getTagOrEmpty(dough)) {
                var input = CraftingInput.of(3, 1, List.of(new ItemStack(vegetable.value()),
                        new ItemStack(PufferfishItems.SASHIMI.get()), new ItemStack(flour.value())));
                var selected = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
                var result = selected.value().assemble(input, level.registryAccess());
                helper.assertTrue(result.is(PufferfishItems.RAW_BAOZI.get()) && result.getCount() == 1,
                        "Tagged ingredients conflicted with another recipe");
                checked++;
            }
        }
        helper.assertTrue(checked > 1, "Expected multiple supported vegetable and dough combinations");
        var raw = new ItemStack(PufferfishItems.RAW_BAOZI.get());
        helper.assertTrue(!raw.is(dough), "Filled baozi must not match the plain dough tag");
        var steamer = level.getRecipeManager().getRecipeFor(ModRecipes.STEAMER_RECIPE,
                new net.minecraft.world.item.crafting.SingleRecipeInput(raw), level).orElseThrow().value();
        helper.assertTrue(steamer.getCookTick() == 1200 && steamer.getResult().is(PufferfishItems.BAOZI.get()),
                "Baozi matched the wrong steaming recipe");
        var original = level.getRecipeManager().getRecipeFor(ModRecipes.STEAMER_RECIPE,
                new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(ModItems.STUFFED_DOUGH_FOOD.get())), level)
                .orElseThrow().value();
        helper.assertTrue(original.getResult().is(ModItems.BAOZI.get()), "Original baozi recipe was changed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "pufferfish_please", timeoutTicks = 1260)
    public static void steamerCooksAndReturnsFourBaozi(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.CAMPFIRE);
        helper.setBlock(pos, ModBlocks.STEAMER.get().defaultBlockState()
                .setValue(com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock.HALF, true));
        var steamer = (com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.SteamerBlockEntity)
                level.getBlockEntity(helper.absolutePos(pos));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var input = new ItemStack(PufferfishItems.RAW_BAOZI.get(), 4);
        helper.assertTrue(steamer.placeFood(level, player, input) && input.isEmpty(), "Steamer did not accept four raw baozi");
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(steamer.getItems().getFirst().is(PufferfishItems.RAW_BAOZI.get()), "Uncovered baozi cooked");
            level.setBlockAndUpdate(helper.absolutePos(pos), steamer.getBlockState()
                    .setValue(com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock.HAS_LID, true));
        });
        helper.runAtTickTime(1200, () -> helper.assertTrue(steamer.getItems().getFirst().is(PufferfishItems.RAW_BAOZI.get()),
                "Baozi cooked before sixty seconds"));
        helper.runAtTickTime(1230, () -> {
            for (int i = 0; i < 4; i++) helper.assertTrue(steamer.getItems().get(i).is(PufferfishItems.BAOZI.get())
                    && steamer.getItems().get(i).getCount() == 1, "Steamer lost or failed to cook a baozi");
            level.setBlockAndUpdate(helper.absolutePos(pos), steamer.getBlockState()
                    .setValue(com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock.HAS_LID, false));
            helper.assertTrue(steamer.takeFood(level, player) && player.getInventory().countItem(PufferfishItems.BAOZI.get()) == 4
                    && steamer.getItems().stream().allMatch(ItemStack::isEmpty), "Steamer did not return exactly four baozi");
            helper.succeed();
        });
    }

    /// Verifies sashimi works in strict dishes and in flexible soups mixed with whole fish.
    @GameTest(template = "empty", templateNamespace = "pufferfish_please")
    public static void sashimiWorksInMixedDishes(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var stacks = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 4; i++) stacks.set(i, new ItemStack(PufferfishItems.SASHIMI.get()));
        var pot = new SimpleInput(stacks);
        helper.assertTrue(manager.getRecipeFor(ModRecipes.POT_RECIPE, pot, level).orElseThrow().value()
                .assemble(pot, level.registryAccess()).is(PufferfishItems.BRAISED.get()), "Sashimi did not work in stir-frying");
        for (int i = 2; i < 5; i++) stacks.set(i, new ItemStack(ModItems.LETTUCE.get()));
        var input = new StockpotInput(stacks, ResourceLocation.withDefaultNamespace("water"));
        helper.assertTrue(manager.getRecipeFor(ModRecipes.STOCKPOT_RECIPE, input, level).orElseThrow().value()
                .assemble(input, level.registryAccess()).is(ModItems.PUFFERFISH_SOUP.get()), "Sashimi did not work in strict soup");
        var rice = BuiltInRegistries.ITEM.get(ResourceLocation.parse("kaleidoscope_cookery:rice"));
        stacks.set(5, new ItemStack(rice));
        helper.assertTrue(manager.getRecipeFor(ModRecipes.STOCKPOT_RECIPE, input, level).orElseThrow().value()
                .assemble(input, level.registryAccess()).is(PufferfishItems.ZOSUI.get()), "Sashimi did not work in strict zosui");
        stacks.set(5, new ItemStack(Items.PUFFERFISH));
        helper.assertTrue(manager.getRecipeFor(ModRecipes.FLEX_STOCKPOT_RECIPE, input, level).orElseThrow().value()
                .assemble(input, level.registryAccess()).is(ModItems.PUFFERFISH_SOUP.get()), "Whole fish and sashimi did not work in flexible soup");
        stacks.set(6, new ItemStack(rice));
        helper.assertTrue(manager.getRecipeFor(ModRecipes.FLEX_STOCKPOT_RECIPE, input, level).orElseThrow().value()
                .assemble(input, level.registryAccess()).is(PufferfishItems.ZOSUI.get()), "Whole fish and sashimi did not work in flexible zosui");
        helper.succeed();
    }

    /// Verifies raw rice upgrades all fish soup variants while preserving the original recipes without rice.
    @GameTest(template = "empty", templateNamespace = "pufferfish_please")
    public static void riceTurnsFishSoupIntoZosui(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        for (var riceId : List.of("kaleidoscope_cookery:rice", "farmersdelight:rice")) {
            var location = ResourceLocation.parse(riceId);
            if (!ModList.get().isLoaded(location.getNamespace())) continue;
            for (boolean bucket : new boolean[] {false, true}) {
                for (int color = -1; color < 16; color++) {
                    for (int sliced = 0; sliced <= 2; sliced++) {
                        var stacks = NonNullList.withSize(9, ItemStack.EMPTY);
                        int index = 0;
                        if (!bucket) {
                            for (int i = 0; i < 2; i++) stacks.set(index++, i < sliced ? new ItemStack(PufferfishItems.SASHIMI.get())
                                    : CuttingAssertions.fish(color < 0 ? null : DyeColor.byId(color)).copyWithCount(1));
                        }
                        for (int i = 0; i < 3; i++) stacks.set(index++, new ItemStack(ModItems.LETTUCE.get()));
                        stacks.set(index, new ItemStack(BuiltInRegistries.ITEM.get(location)));
                        var base = bucket ? ResourceLocation.withDefaultNamespace("pufferfish_bucket")
                                : ResourceLocation.withDefaultNamespace("water");
                        var input = new StockpotInput(stacks, base);
                        var strict = manager.getRecipeFor(ModRecipes.STOCKPOT_RECIPE, input, level).orElseThrow().value();
                        var flexible = manager.getRecipeFor(ModRecipes.FLEX_STOCKPOT_RECIPE, input, level).orElseThrow().value();
                        helper.assertTrue(strict.assemble(input, level.registryAccess()).is(PufferfishItems.ZOSUI.get())
                                && flexible.assemble(input, level.registryAccess()).is(PufferfishItems.ZOSUI.get()),
                                "Rice did not upgrade a whole, sliced, mixed, or bucket-based soup to zosui");
                        for (var invalid : List.of(Items.WHEAT, ModItems.COOKED_RICE.get(), PufferfishItems.BRAISED_RICE_BOWL.get())) {
                            stacks.set(index, new ItemStack(invalid));
                            helper.assertTrue(!strict.matches(input, level) && !flexible.matches(input, level), "Zosui accepted cooked rice or another grain");
                        }
                        stacks.set(index, ItemStack.EMPTY);
                        helper.assertTrue(manager.getRecipeFor(ModRecipes.STOCKPOT_RECIPE, input, level).orElseThrow().value()
                                .assemble(input, level.registryAccess()).is(ModItems.PUFFERFISH_SOUP.get()), "Rice-free soup changed its result");
                        helper.assertTrue(manager.getRecipeFor(ModRecipes.FLEX_STOCKPOT_RECIPE, input, level).orElseThrow().value()
                                .assemble(input, level.registryAccess()).is(ModItems.PUFFERFISH_SOUP.get()), "Rice-free flexible soup changed its result");
                    }
                }
            }
        }
        helper.succeed();
    }

    /// Verifies four interchangeable fish ingredients, rejects other counts, and cooks a mixed batch in a real pot.
    @GameTest(template = "empty", templateNamespace = "pufferfish_please")
    public static void fourFishStirFryIntoOneDish(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = (PotRecipe) level.getRecipeManager().byKey(PufferfishPlease.id("compat/cookery/braised_pufferfish"))
                .orElseThrow().value();
        for (int color = -1; color < 16; color++) {
            for (int sliced = 0; sliced <= 4; sliced++) {
                var inputs = NonNullList.withSize(9, ItemStack.EMPTY);
                for (int i = 0; i < 4; i++) inputs.set(i, i < sliced ? new ItemStack(PufferfishItems.SASHIMI.get())
                        : CuttingAssertions.fish(color < 0 ? null : DyeColor.byId(color)).copyWithCount(1));
                var input = new SimpleInput(inputs);
                var selected = level.getRecipeManager().getRecipeFor(ModRecipes.POT_RECIPE, input, level).orElseThrow();
                helper.assertTrue(selected.value().assemble(input, level.registryAccess()).is(PufferfishItems.BRAISED.get()), "Mixed fish produced the wrong dish");
                inputs.set(3, ItemStack.EMPTY);
                helper.assertTrue(!recipe.matches(input, level), "Three fish produced a four-fish dish");
                inputs.set(3, new ItemStack(PufferfishItems.SASHIMI.get()));
                inputs.set(4, new ItemStack(PufferfishItems.SASHIMI.get()));
                helper.assertTrue(!recipe.matches(input, level), "Five fish matched an exact four-fish recipe");
                inputs.set(4, ItemStack.EMPTY);
                inputs.set(3, new ItemStack(PufferfishItems.HEAVY.get()));
                helper.assertTrue(!recipe.matches(input, level), "Equipment was accepted as cooking meat");
            }
        }
        var pos = helper.absolutePos(new BlockPos(2, 2, 1));
        level.setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        level.setBlockAndUpdate(pos, ModBlocks.POT.get().defaultBlockState());
        var pot = (PotBlockEntity) level.getBlockEntity(pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var oil = new ItemStack(ModItems.OIL.get());
        helper.assertTrue(pot.onPlaceOil(level, player, oil) && oil.isEmpty(), "Pot did not consume cooking oil");
        var whole = CuttingAssertions.fish(DyeColor.BLUE);
        var sliced = new ItemStack(PufferfishItems.SASHIMI.get(), 2);
        for (var stack : List.of(whole, whole, sliced, sliced)) helper.assertTrue(pot.addIngredient(level, player, stack), "Pot rejected fish ingredients");
        helper.assertTrue(whole.isEmpty() && sliced.isEmpty(), "Pot did not consume four fish");
        for (int i = 0; i < recipe.stirFryCount(); i++) pot.onShovelHit(level, player, new ItemStack(ModItems.KITCHEN_SHOVEL.get()));
        for (int i = 0; i <= recipe.time(); i++) pot.tick(level);
        var bowl = new ItemStack(Items.BOWL);
        helper.assertTrue(pot.takeOutProduct(level, player, bowl) && bowl.isEmpty()
                && player.getInventory().countItem(PufferfishItems.BRAISED.get()) == 1, "Cooked fish did not fill exactly one bowl");
        helper.succeed();
    }

    /// Verifies both mods' cooked rice, native quality transfer, and one surplus bowl after crafting.
    @GameTest(template = "empty", templateNamespace = "pufferfish_please")
    public static void braisedFishCombinesWithCookedRice(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var id : List.of("kaleidoscope_cookery:cooked_rice", "farmersdelight:cooked_rice")) {
            var location = ResourceLocation.parse(id);
            if (!ModList.get().isLoaded(location.getNamespace())) continue;
            var rice = new ItemStack(BuiltInRegistries.ITEM.get(location));
            var dish = new ItemStack(PufferfishItems.BRAISED.get());
            QualityUtils.setQuality(dish, Quality.SUPERB);
            for (var ingredients : List.of(List.of(dish, rice), List.of(rice, dish))) {
                var input = CraftingInput.of(2, 1, ingredients);
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().value();
                var output = recipe.assemble(input, level.registryAccess());
                helper.assertTrue(output.is(PufferfishItems.BRAISED_RICE_BOWL.get()) && output.getCount() == 1
                        && QualityUtils.getQuality(output) == Quality.SUPERB, "Rice bowl lost its output or ingredient quality");
                var remains = recipe.getRemainingItems(input);
                helper.assertTrue(remains.stream().filter(stack -> stack.is(Items.BOWL)).mapToInt(ItemStack::getCount).sum() == 1,
                        "Combining two bowls must return exactly one surplus bowl");
                helper.assertTrue(!recipe.matches(CraftingInput.of(2, 1, List.of(dish, new ItemStack(Items.WHEAT))), level), "Uncooked grain replaced cooked rice");
            }
        }
        helper.succeed();
    }
}
