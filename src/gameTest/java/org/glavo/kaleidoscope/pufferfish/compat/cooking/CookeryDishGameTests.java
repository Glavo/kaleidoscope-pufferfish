// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.cooking;

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
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishCuttingGameTests;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

/// Exercises stir-frying, rice soup, and Cookery's rice bowl crafting and container behavior.
@PrefixGameTestTemplate(false)
public final class CookeryDishGameTests {
    /// Prevents construction of the test holder.
    private CookeryDishGameTests() {}

    /// Verifies golden slices work in strict dishes and in flexible soups mixing all three raw fish items.
    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void goldenSashimiWorksInMixedDishes(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var stacks = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 4; i++) stacks.set(i, new ItemStack(PufferfishItems.GOLDEN_SASHIMI.get()));
        var pot = new SimpleInput(stacks);
        helper.assertTrue(manager.getRecipeFor(ModRecipes.POT_RECIPE, pot, level).orElseThrow().value()
                .assemble(pot, level.registryAccess()).is(PufferfishItems.BRAISED.get()), "Golden sashimi did not work in stir-frying");
        stacks.set(1, new ItemStack(PufferfishItems.SASHIMI.get()));
        for (int i = 2; i < 5; i++) stacks.set(i, new ItemStack(ModItems.LETTUCE.get()));
        var input = new StockpotInput(stacks, ResourceLocation.withDefaultNamespace("water"));
        helper.assertTrue(manager.getRecipeFor(ModRecipes.STOCKPOT_RECIPE, input, level).orElseThrow().value()
                .assemble(input, level.registryAccess()).is(ModItems.PUFFERFISH_SOUP.get()), "Golden sashimi did not work in strict soup");
        var rice = BuiltInRegistries.ITEM.get(ResourceLocation.parse("kaleidoscope_cookery:rice"));
        stacks.set(5, new ItemStack(rice));
        helper.assertTrue(manager.getRecipeFor(ModRecipes.STOCKPOT_RECIPE, input, level).orElseThrow().value()
                .assemble(input, level.registryAccess()).is(PufferfishItems.ZOSUI.get()), "Golden sashimi did not work in strict zosui");
        stacks.set(5, new ItemStack(Items.PUFFERFISH));
        helper.assertTrue(manager.getRecipeFor(ModRecipes.FLEX_STOCKPOT_RECIPE, input, level).orElseThrow().value()
                .assemble(input, level.registryAccess()).is(ModItems.PUFFERFISH_SOUP.get()), "Three fish types did not work in flexible soup");
        stacks.set(6, new ItemStack(rice));
        helper.assertTrue(manager.getRecipeFor(ModRecipes.FLEX_STOCKPOT_RECIPE, input, level).orElseThrow().value()
                .assemble(input, level.registryAccess()).is(PufferfishItems.ZOSUI.get()), "Three fish types did not work in flexible zosui");
        helper.succeed();
    }

    /// Verifies raw rice upgrades all fish soup variants while preserving the original recipes without rice.
    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
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
                                    : PufferfishCuttingGameTests.fish(color < 0 ? null : DyeColor.byId(color)).copyWithCount(1));
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
    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void fourFishStirFryIntoOneDish(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = (PotRecipe) level.getRecipeManager().byKey(KaleidoscopePufferfish.id("compat/cookery/braised_pufferfish"))
                .orElseThrow().value();
        for (int color = -1; color < 16; color++) {
            for (int sliced = 0; sliced <= 4; sliced++) {
                var inputs = NonNullList.withSize(9, ItemStack.EMPTY);
                for (int i = 0; i < 4; i++) inputs.set(i, i < sliced ? new ItemStack(PufferfishItems.SASHIMI.get())
                        : PufferfishCuttingGameTests.fish(color < 0 ? null : DyeColor.byId(color)).copyWithCount(1));
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
        var whole = PufferfishCuttingGameTests.fish(DyeColor.BLUE);
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
    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
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
