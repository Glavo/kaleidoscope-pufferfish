// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.cooking;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.SimpleInput;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishCuttingGameTests;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.glavo.kaleidoscope.pufferfish.compat.cookery.CookeryFoodIntegration;

@PrefixGameTestTemplate(false)
public final class CookeryPieGameTests {
    private CookeryPieGameTests() {}

    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void wholePufferfishMakeTheirOwnStargazyPie(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        for (int color = -1; color < 16; color++) {
            var items = NonNullList.withSize(9, ItemStack.EMPTY);
            items.set(0, new ItemStack(Items.PUMPKIN_PIE));
            for (int i = 1; i <= 5; i++) items.set(i, PufferfishCuttingGameTests.fish(color < 0 ? null : DyeColor.byId(color)).copyWithCount(1));
            var input = new SimpleInput(items);
            var recipe = manager.getRecipeFor(ModRecipes.POT_RECIPE, input, level).orElseThrow();
            helper.assertTrue(recipe.id().equals(KaleidoscopePufferfish.id("compat/cookery/pufferfish_stargazy_pie"))
                    && recipe.value().assemble(input, level.registryAccess()).is(CookeryFoodIntegration.STARGAZY_PIE_ITEM.get()),
                    "Whole or dyed pufferfish matched the wrong pie");
            for (int i = 2; i <= 5; i++) items.set(i, ItemStack.EMPTY);
            helper.assertTrue(manager.getRecipeFor(ModRecipes.FLEX_POT_RECIPE, input, level).orElseThrow().value()
                    .assemble(input, level.registryAccess()).is(CookeryFoodIntegration.STARGAZY_PIE_ITEM.get()),
                    "Flexible pie recipe rejected pufferfish");
        }
        var original = BuiltInRegistries.ITEM.get(ResourceLocation.parse("kaleidoscope_cookery:stargazy_pie"));
        var items = NonNullList.withSize(9, ItemStack.EMPTY);
        items.set(0, new ItemStack(Items.PUMPKIN_PIE));
        for (int i = 1; i <= 5; i++) items.set(i, new ItemStack(Items.COD));
        helper.assertTrue(manager.getRecipeFor(ModRecipes.POT_RECIPE, new SimpleInput(items), level).orElseThrow().value()
                .assemble(new SimpleInput(items), level.registryAccess()).is(original), "Original stargazy pie was overridden");
        var own = manager.byKey(KaleidoscopePufferfish.id("compat/cookery/pufferfish_stargazy_pie")).orElseThrow();
        for (var invalid : List.of(PufferfishItems.SASHIMI.get(), PufferfishItems.GOLDEN.get(), PufferfishItems.NETHERITE.get(), PufferfishItems.HEAVY.get())) {
            for (int i = 1; i <= 5; i++) items.set(i, new ItemStack(invalid));
            helper.assertTrue(!((com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.PotRecipe) own.value())
                    .matches(new SimpleInput(items), level), "Pie accepted sliced or equipment fish");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void potCooksPufferfishPieIntoOneBowl(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        level.setBlockAndUpdate(pos, ModBlocks.POT.get().defaultBlockState());
        var pot = (PotBlockEntity) level.getBlockEntity(pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(pot.onPlaceOil(level, player, new ItemStack(ModItems.OIL.get())), "Pot rejected oil");
        helper.assertTrue(pot.addIngredient(level, player, new ItemStack(Items.PUMPKIN_PIE)), "Pot rejected pumpkin pie");
        for (int i = 0; i < 5; i++) {
            var fish = PufferfishCuttingGameTests.fish(DyeColor.byId(i)).copyWithCount(1);
            helper.assertTrue(pot.addIngredient(level, player, fish) && fish.isEmpty(), "Pot did not consume a colored fish");
        }
        for (int i = 0; i < 3; i++) pot.onShovelHit(level, player, new ItemStack(ModItems.KITCHEN_SHOVEL.get()));
        for (int i = 0; i <= 200; i++) pot.tick(level);
        var bowl = new ItemStack(Items.BOWL);
        helper.assertTrue(pot.takeOutProduct(level, player, bowl) && bowl.isEmpty()
                && player.getInventory().countItem(CookeryFoodIntegration.STARGAZY_PIE_ITEM.get()) == 1,
                "Pot did not produce one pufferfish pie in its bowl");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "kaleidoscope_pufferfish")
    public static void piePreservesQualityAndCannotDuplicateFood(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var block = CookeryFoodIntegration.STARGAZY_PIE.get();
        var item = CookeryFoodIntegration.STARGAZY_PIE_ITEM.get();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos.below(), false);
        var stack = new ItemStack(item);
        QualityUtils.setQuality(stack, Quality.SUPERB);
        var state = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
        helper.assertTrue(state.getValue(FoodBiteBlock.QUALITY) == Quality.SUPERB.getId(), "Placement lost pie quality");
        var drops = Block.getDrops(state, level, pos, null);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item)
                && QualityUtils.getQuality(drops.getFirst()) == Quality.SUPERB, "Whole pie drop lost its quality");
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        for (int bite = 0; bite < 4; bite++) {
            player.getFoodData().setFoodLevel(0);
            player.removeAllEffects();
            level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            helper.assertTrue(player.getFoodData().getFoodLevel() == 3
                    && level.getBlockState(pos).getValue(block.getBites()) == bite + 1, "Pie portion was not consumed");
            var poison = player.getEffect(MobEffects.POISON);
            helper.assertTrue(poison != null && poison.getDuration() == 60 && poison.getAmplifier() == 0
                    && player.hasEffect(MobEffects.UNLUCK) && player.getActiveEffects().size() == 3,
                    "Pie portion did not add Poison I while preserving its original effects");
            drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(Items.BOWL), "Partially eaten pie returned edible food");
        }
        level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        helper.assertTrue(level.getBlockState(pos).isAir(), "Empty pie dish could not be removed");
        player.getFoodData().setFoodLevel(0);
        player.removeAllEffects();
        var remainder = new ItemStack(item).finishUsingItem(level, player);
        var poison = player.getEffect(MobEffects.POISON);
        helper.assertTrue(remainder.is(Items.BOWL) && player.getFoodData().getFoodLevel() == 13
                && player.hasEffect(MobEffects.UNLUCK) && poison != null && poison.getDuration() == 60
                && poison.getAmplifier() == 0 && player.getActiveEffects().size() == 3,
                "Whole pie lost its poison, original effects, nutrition, or bowl return");
        player.removeAllEffects();
        stack.finishUsingItem(level, player);
        helper.assertTrue(player.getEffect(MobEffects.POISON).getDuration() == 72, "Whole pie poison ignored Superb quality");
        player.removeAllEffects();
        level.setBlockAndUpdate(pos, state);
        state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        helper.assertTrue(player.getEffect(MobEffects.POISON).getDuration() == 72, "Placed pie poison ignored Superb quality");
        helper.succeed();
    }
}
