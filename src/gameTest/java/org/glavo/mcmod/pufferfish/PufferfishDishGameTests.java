// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/// Verifies cooked meals, their bowl returns, and optional recipe loading.
@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishDishGameTests {
    private PufferfishDishGameTests() {}

    @GameTest(template = "empty")
    public static void aspicRecipesRequireChineseFood(GameTestHelper helper) {
        boolean cookery = ModList.get().isLoaded("kaleidoscope_cookery");
        boolean chineseFood = ModList.get().isLoaded("kaleidoscope_chinesefood");
        var manager = helper.getLevel().getRecipeManager();
        helper.assertTrue(manager.byKey(PufferfishPlease.id("compat/cookery/pufferfish_skin_broth")).isPresent() == (cookery && chineseFood)
                && manager.byKey(PufferfishPlease.id("compat/chinesefood/pufferfish_aspic")).isPresent() == (cookery && chineseFood),
                "Aspic processing ignored its optional dependencies");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(PufferfishPlease.id("pufferfish_aspic")) == cookery,
                "Aspic ignored its optional food block dependency");
        helper.assertTrue(PufferfishItems.SKIN_BROTH.get().getDefaultInstance().getFoodProperties(null) == null,
                "Unchilled broth should not be edible");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void baoziProvidesWaterBreathingWithoutContainers(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(PufferfishItems.RAW_BAOZI.get().getDefaultInstance().getFoodProperties(player) == null,
                "Raw baozi should not be edible");
        for (int count : new int[] {1, 2}) {
            player.getFoodData().setFoodLevel(0);
            var remaining = new ItemStack(PufferfishItems.BAOZI.get(), count).finishUsingItem(helper.getLevel(), player);
            var breathing = player.getEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING);
            helper.assertTrue(player.getFoodData().getFoodLevel() == 8 && breathing != null
                    && breathing.getDuration() == 1200 && breathing.getAmplifier() == 0
                    && player.getActiveEffects().size() == 1, "Baozi food or effects are incorrect");
            helper.assertTrue(remaining.getCount() == count - 1 && player.getInventory().countItem(Items.BOWL) == 0,
                    "Baozi consumption produced an unexpected remainder");
        }
        for (var name : List.of("raw_pufferfish_baozi", "pufferfish_baozi")) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(PufferfishPlease.id("compat/cookery/" + name)).isPresent()
                    == ModList.get().isLoaded("kaleidoscope_cookery"), "Baozi recipe ignored its mod condition");
        }
        helper.succeed();
    }

    /// Verifies food recovery and bowl returns for single and stacked meals without applying raw-fish poison.
    @GameTest(template = "empty")
    public static void cookedDishesRestoreFoodAndReturnBowls(GameTestHelper helper) {
        var pie = PufferfishPlease.id("pufferfish_stargazy_pie");
        boolean cookery = ModList.get().isLoaded("kaleidoscope_cookery");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(pie) == cookery
                && net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(pie) == cookery,
                "Placeable pie ignored its optional Cookery dependency");
        for (var name : List.of("pufferfish_stargazy_pie", "pufferfish_stargazy_pie_flexible")) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(PufferfishPlease.id("compat/cookery/" + name)).isPresent()
                    == cookery, "Pie recipe ignored its mod condition");
        }
        for (var item : List.of(PufferfishItems.BRAISED.get(), PufferfishItems.BRAISED_RICE_BOWL.get(), PufferfishItems.ZOSUI.get())) {
            for (int count : new int[] {1, 2}) {
                var player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.getFoodData().setFoodLevel(0);
                var stack = new ItemStack(item, count);
                var result = stack.finishUsingItem(helper.getLevel(), player);
                int nutrition = item == PufferfishItems.BRAISED.get() ? 9 : item == PufferfishItems.ZOSUI.get() ? 10 : 14;
                helper.assertTrue(player.getFoodData().getFoodLevel() == nutrition
                        && player.getActiveEffects().isEmpty() && !PufferfishItems.isWeapon(stack), "Cooked meal nutrition or effects are incorrect");
                helper.assertTrue(count == 1 ? result.is(Items.BOWL) && result.getCount() == 1
                        : result.is(item) && result.getCount() == 1 && player.getInventory().countItem(Items.BOWL) == 1,
                        "Eating a meal lost or duplicated its bowl");
            }
        }
        for (var name : List.of("braised_pufferfish", "braised_pufferfish_rice_bowl", "pufferfish_zosui",
                "pufferfish_zosui_flexible", "pufferfish_zosui_mixed", "pufferfish_zosui_entity", "pufferfish_zosui_entity_flexible")) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(PufferfishPlease.id("compat/cookery/" + name)).isPresent()
                    == ModList.get().isLoaded("kaleidoscope_cookery"), "Cookery dish recipe ignored its mod condition");
        }
        helper.succeed();
    }
}
