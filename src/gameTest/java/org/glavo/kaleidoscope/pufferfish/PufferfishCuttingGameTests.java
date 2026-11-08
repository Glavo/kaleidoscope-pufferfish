// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
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
        if (ModList.get().isLoaded("farmersdelight")) event.register(FarmersDelightCuttingGameTests.class);
    }

    /// Verifies recipe conditions for all colors and the meat's food-only behavior.
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
        var meat = new ItemStack(PufferfishItems.MEAT.get());
        var food = meat.getFoodProperties(null);
        helper.assertTrue(food != null && food.nutrition() == 2 && food.effects().isEmpty()
                && !PufferfishItems.isWeapon(meat), "Meat must be plain food, not a throwable fish");
        helper.succeed();
    }

    /// Creates two fish of the supplied color so consumption can be checked independently.
    public static ItemStack fish(DyeColor color) {
        var stack = new ItemStack(Items.PUFFERFISH, 2);
        return color == null ? stack : PufferfishColors.dye(stack, color);
    }

    /// Checks for exactly one meat and one matching skin, then removes the dropped entities.
    public static void verifyDrops(GameTestHelper helper, BlockPos pos, DyeColor color) {
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        helper.assertTrue(drops.size() == 2
                && drops.stream().allMatch(entity -> entity.getItem().getCount() == 1)
                && drops.stream().anyMatch(entity -> entity.getItem().is(PufferfishItems.MEAT.get()))
                && drops.stream().anyMatch(entity -> entity.getItem().is(PufferfishBlocks.skin(color).asItem())),
                "Cutting must drop exactly one meat and one matching skin");
        drops.forEach(ItemEntity::discard);
    }
}
