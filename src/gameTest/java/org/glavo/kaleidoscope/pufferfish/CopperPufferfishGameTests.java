// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class CopperPufferfishGameTests {
    @GameTest(template = "empty")
    public static void waxingPreservesComponentsAndRejectsWaxedFish(GameTestHelper helper) {
        for (int i = 0; i < 8; i++) {
            var fish = new ItemStack(PufferfishItems.COPPER_VARIANTS.get(i).get(), 8);
            fish.set(DataComponents.CUSTOM_NAME, Component.literal("Copper test"));
            var input = CraftingInput.of(2, 1, List.of(fish, new ItemStack(Items.HONEYCOMB)));
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
            helper.assertTrue(recipe.isPresent() == (i < 4), "Unexpected waxing recipe match");
            if (i < 4) {
                var result = recipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
                helper.assertTrue(CopperPufferfish.index(result) == i + 4 && result.getCount() == 1
                        && result.get(DataComponents.CUSTOM_NAME).equals(fish.get(DataComponents.CUSTOM_NAME)), "Waxing lost fish data");
            }
            helper.assertTrue(PufferfishItems.canThrow(fish)
                    && PufferfishCombat.damage(fish) == PufferfishCombat.damage(new ItemStack(PufferfishItems.COPPER.get())),
                    "Copper variants have inconsistent combat properties");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placedFishOxidizeAndAxesRemoveWaxBeforeRust(GameTestHelper helper) {
        var relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, PufferfishBlocks.PLACED_FISH.get());
        var pos = helper.absolutePos(relative);
        var placed = (PlacedPufferfishBlockEntity) helper.getLevel().getBlockEntity(pos);
        placed.setFish(new ItemStack(PufferfishItems.COPPER.get()));
        var random = RandomSource.create(42);
        for (int i = 0; i < 10000; i++) CopperPufferfish.randomTick(helper.getLevel(), pos, random);
        helper.assertTrue(CopperPufferfish.index(placed.fish()) == 3, "Copper fish did not fully oxidize");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, 2));
        use(player, pos);
        helper.assertTrue(CopperPufferfish.index(placed.fish()) == 7 && player.getMainHandItem().getCount() == 1, "Waxing failed");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        use(player, pos);
        helper.assertTrue(CopperPufferfish.index(placed.fish()) == 3, "Axe removed oxidation before wax");
        use(player, pos);
        helper.assertTrue(CopperPufferfish.index(placed.fish()) == 2 && player.getMainHandItem().getDamageValue() == 2, "Scraping failed");
        for (int stage = 0; stage < 4; stage++) {
            placed.setFish(new ItemStack(PufferfishItems.COPPER_VARIANTS.get(stage + 4).get()));
            for (int i = 0; i < 10000; i++) CopperPufferfish.randomTick(helper.getLevel(), pos, random);
            helper.assertTrue(CopperPufferfish.index(placed.fish()) == stage + 4, "Waxed fish oxidized");
        }
        helper.succeed();
    }

    private static void use(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }
}
