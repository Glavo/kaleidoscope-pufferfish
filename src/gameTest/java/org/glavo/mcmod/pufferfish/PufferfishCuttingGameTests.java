// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import static org.glavo.mcmod.pufferfish.test.CuttingAssertions.fish;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.compat.cooking.CookeryCuttingGameTests;
import org.glavo.mcmod.pufferfish.compat.cooking.CookeryDishGameTests;
import org.glavo.mcmod.pufferfish.compat.cooking.FarmersDelightCuttingGameTests;

/// Verifies optional cutting recipes and provides assertions shared by both board integrations.
@EventBusSubscriber(modid = PufferfishPlease.MODID)
@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishCuttingGameTests {
    private PufferfishCuttingGameTests() {}

    /// Registers board tests only when their respective optional mods are loaded.
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("kaleidoscope_chinesefood")) event.register(org.glavo.mcmod.pufferfish.compat.chinesefood.PufferfishAspicGameTests.class);
        if (ModList.get().isLoaded("kaleidoscope_cookery")) event.register(CookeryCuttingGameTests.class);
        if (ModList.get().isLoaded("kaleidoscope_cookery")) event.register(CookeryDishGameTests.class);
        if (ModList.get().isLoaded("kaleidoscope_cookery")) event.register(org.glavo.mcmod.pufferfish.compat.cooking.CookeryPieGameTests.class);
        if (ModList.get().isLoaded("farmersdelight")) event.register(FarmersDelightCuttingGameTests.class);
    }

    /// Verifies recipe conditions for all colors and eating sashimi without status effects.
    @GameTest(template = "empty")
    public static void cuttingRecipesFollowOptionalMods(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (int index = -1; index < 16; index++) {
            var name = PufferfishBlocks.skinName(index < 0 ? null : DyeColor.byId(index));
            helper.assertTrue(manager.byKey(PufferfishPlease.id("chopping/" + name)).isPresent()
                    == ModList.get().isLoaded("kaleidoscope_cookery"), "Cookery recipe condition failed");
            helper.assertTrue(manager.byKey(PufferfishPlease.id("cutting/" + name)).isPresent()
                    == ModList.get().isLoaded("farmersdelight"), "Farmer's Delight recipe condition failed");
        }
        var sashimi = new ItemStack(PufferfishItems.SASHIMI.get(), 2);
        var food = sashimi.getFoodProperties(null);
        helper.assertTrue(food != null && food.nutrition() == 2 && food.effects().isEmpty()
                && !PufferfishItems.isWeapon(sashimi), "Sashimi must remain food rather than a throwable fish");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        sashimi.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(sashimi.getCount() == 1 && player.getFoodData().getFoodLevel() == 12
                && player.getActiveEffects().isEmpty(),
                "Eating sashimi must consume one item and restore food without applying status effects");
        helper.succeed();
    }

    /// Verifies safe sashimi is accepted by cooking recipes while poisonous whole fish remain excluded.
    @GameTest(template = "empty")
    public static void cookingTagsDistinguishFoodFromEquipment(GameTestHelper helper) {
        var pufferfish = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/raw_pufferfish"));
        var rawFish = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/raw_fish"));
        var safeFish = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/safe_raw_fish"));
        var sashimi = new ItemStack(PufferfishItems.SASHIMI.get());
        helper.assertTrue(sashimi.is(pufferfish) && sashimi.is(rawFish) && sashimi.is(safeFish), "Safe sashimi cooking tags are incorrect");
        for (int index = -1; index < 16; index++) {
            var fish = fish(index < 0 ? null : DyeColor.byId(index));
            helper.assertTrue(fish.is(pufferfish) && fish.is(rawFish) && !fish.is(safeFish), "Whole fish cooking tags are incorrect");
        }
        for (var item : new net.minecraft.world.item.Item[] {PufferfishItems.NETHERITE.get(), PufferfishItems.HEAVY.get(),
                PufferfishItems.GOLDEN.get(), PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get(), Items.PUFFERFISH_BUCKET}) {
            helper.assertTrue(!new ItemStack(item).is(pufferfish), "Equipment or special fish entered the raw pufferfish tag");
        }
        var manager = helper.getLevel().getRecipeManager();
        var soup = PufferfishPlease.id("compat/cookery/pufferfish_soup_from_sashimi");
        helper.assertTrue(manager.byKey(soup).isPresent() == ModList.get().isLoaded("kaleidoscope_cookery"), "Sashimi soup recipe ignored its mod condition");
        for (var name : java.util.List.of("golden_pufferfish", "netherite_pufferfish_skin", "heavy_pufferfish_skin",
                "copper_pufferfish_skin", "iron_pufferfish_skin", "diamond_pufferfish_skin")) {
            helper.assertTrue(manager.byKey(PufferfishPlease.id("chopping/" + name)).isPresent()
                    == ModList.get().isLoaded("kaleidoscope_cookery"), "Special chopping recipe ignored its mod condition");
            helper.assertTrue(manager.byKey(PufferfishPlease.id("cutting/" + name)).isPresent()
                    == ModList.get().isLoaded("farmersdelight"), "Special cutting recipe ignored its mod condition");
        }
        if (ModList.get().isLoaded("farmersdelight")) {
            var stew = manager.byKey(ResourceLocation.fromNamespaceAndPath("farmersdelight", "cooking/fish_stew")).orElseThrow().value();
            helper.assertTrue(stew.getIngredients().getFirst().test(sashimi)
                    && !stew.getIngredients().getFirst().test(new ItemStack(Items.PUFFERFISH)), "Fish stew did not preserve safe-fish filtering");
        }
        helper.succeed();
    }

}
