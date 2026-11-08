// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.compat.cooking.CookeryCuttingGameTests;
import org.glavo.kaleidoscope.pufferfish.compat.cooking.CookeryDishGameTests;
import org.glavo.kaleidoscope.pufferfish.compat.cooking.FarmersDelightCuttingGameTests;

/// Verifies optional cutting recipes and provides assertions shared by both board integrations.
@EventBusSubscriber(modid = KaleidoscopePufferfish.MODID)
@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishCuttingGameTests {
    /// Prevents construction of the test holder.
    private PufferfishCuttingGameTests() {}

    /// Registers board tests only when their respective optional mods are loaded.
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("kaleidoscope_cookery")) event.register(CookeryCuttingGameTests.class);
        if (ModList.get().isLoaded("kaleidoscope_cookery")) event.register(CookeryDishGameTests.class);
        if (ModList.get().isLoaded("farmersdelight")) event.register(FarmersDelightCuttingGameTests.class);
    }

    /// Verifies recipe conditions for all colors and the sashimi's poisonous food behavior.
    @GameTest(template = "empty")
    public static void cuttingRecipesFollowOptionalMods(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (int index = -1; index < 16; index++) {
            var name = PufferfishBlocks.skinName(index < 0 ? null : DyeColor.byId(index));
            helper.assertTrue(manager.byKey(KaleidoscopePufferfish.id("chopping/" + name)).isPresent()
                    == ModList.get().isLoaded("kaleidoscope_cookery"), "Cookery recipe condition failed");
            helper.assertTrue(manager.byKey(KaleidoscopePufferfish.id("cutting/" + name)).isPresent()
                    == ModList.get().isLoaded("farmersdelight"), "Farmer's Delight recipe condition failed");
        }
        var sashimi = new ItemStack(PufferfishItems.SASHIMI.get(), 2);
        var food = sashimi.getFoodProperties(null);
        helper.assertTrue(food != null && food.nutrition() == 2
                && !PufferfishItems.isWeapon(sashimi), "Sashimi must remain food rather than a throwable fish");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        sashimi.finishUsingItem(helper.getLevel(), player);
        var poison = player.getEffect(MobEffects.POISON);
        helper.assertTrue(sashimi.getCount() == 1 && player.getFoodData().getFoodLevel() == 12
                && poison != null && poison.getDuration() == 1200 && poison.getAmplifier() == 1,
                "Eating sashimi must consume one item, restore food, and apply one minute of Poison II");
        helper.succeed();
    }

    /// Verifies poisonous fish and sashimi are cooking ingredients excluded from safe raw fish recipes.
    @GameTest(template = "empty")
    public static void cookingTagsDistinguishFoodFromEquipment(GameTestHelper helper) {
        var pufferfish = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/raw_pufferfish"));
        var rawFish = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/raw_fish"));
        var safeFish = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/safe_raw_fish"));
        var sashimi = new ItemStack(PufferfishItems.SASHIMI.get());
        helper.assertTrue(sashimi.is(pufferfish) && sashimi.is(rawFish) && !sashimi.is(safeFish), "Poisonous sashimi cooking tags are incorrect");
        for (int index = -1; index < 16; index++) {
            var fish = fish(index < 0 ? null : DyeColor.byId(index));
            helper.assertTrue(fish.is(pufferfish) && fish.is(rawFish) && !fish.is(safeFish), "Whole fish cooking tags are incorrect");
        }
        for (var item : new net.minecraft.world.item.Item[] {PufferfishItems.NETHERITE.get(), PufferfishItems.HEAVY.get(),
                PufferfishItems.GOLDEN.get(), PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), Items.PUFFERFISH_BUCKET}) {
            helper.assertTrue(!new ItemStack(item).is(pufferfish), "Equipment or special fish entered the raw pufferfish tag");
        }
        var manager = helper.getLevel().getRecipeManager();
        var soup = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "stockpot/pufferfish_soup");
        helper.assertTrue(manager.byKey(soup).isPresent() == ModList.get().isLoaded("kaleidoscope_cookery"), "Soup override ignored its mod condition");
        if (ModList.get().isLoaded("farmersdelight")) {
            var stew = manager.byKey(ResourceLocation.fromNamespaceAndPath("farmersdelight", "cooking/fish_stew")).orElseThrow().value();
            helper.assertTrue(!stew.getIngredients().getFirst().test(sashimi)
                    && !stew.getIngredients().getFirst().test(new ItemStack(Items.PUFFERFISH)), "Fish stew did not preserve safe-fish filtering");
        }
        helper.succeed();
    }

    /// Creates two fish of the supplied color so consumption can be checked independently.
    public static ItemStack fish(DyeColor color) {
        var stack = new ItemStack(Items.PUFFERFISH, 2);
        return color == null ? stack : PufferfishColors.dye(stack, color);
    }

    /// Checks for exactly one sashimi and one matching skin, then removes the dropped entities.
    public static void verifyDrops(GameTestHelper helper, BlockPos pos, DyeColor color) {
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        helper.assertTrue(drops.size() == 2
                && drops.stream().allMatch(entity -> entity.getItem().getCount() == 1)
                && drops.stream().anyMatch(entity -> entity.getItem().is(PufferfishItems.SASHIMI.get()))
                && drops.stream().anyMatch(entity -> entity.getItem().is(PufferfishBlocks.skin(color).asItem())),
                "Cutting must drop exactly one sashimi and one matching skin");
        drops.forEach(ItemEntity::discard);
    }
}
