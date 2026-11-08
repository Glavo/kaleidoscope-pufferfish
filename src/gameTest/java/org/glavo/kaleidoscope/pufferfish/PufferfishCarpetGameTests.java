// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/// Verifies color-specific carpet recipes, placement, shape, and drops.
@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishCarpetGameTests {
    /// Prevents construction of the test holder.
    private PufferfishCarpetGameTests() {}

    /// Returns the undyed variant followed by all dye colors.
    private static List<DyeColor> colors() {
        var colors = new ArrayList<DyeColor>();
        colors.add(null);
        colors.addAll(Arrays.asList(DyeColor.values()));
        return colors;
    }

    /// Creates one fish with the supplied color, or an undyed fish if null.
    private static ItemStack fish(DyeColor color) {
        var stack = new ItemStack(Items.PUFFERFISH);
        return color == null ? stack : PufferfishColors.dye(stack, color);
    }

    /// Verifies three same-colored fish in any crafting-table row produce exactly one matching carpet.
    @GameTest(template = "empty")
    public static void threeFishCraftOneMatchingCarpet(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (var color : colors()) {
            for (int row = 0; row < 3; row++) {
                var items = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
                for (int column = 0; column < 3; column++) items.set(row * 3 + column, fish(color));
                var input = CraftingInput.of(3, 3, items);
                var holder = manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
                var output = holder.value().assemble(input, helper.getLevel().registryAccess());
                helper.assertTrue(holder.id().equals(KaleidoscopePufferfish.id(PufferfishBlocks.carpetName(color)))
                        && output.is(PufferfishBlocks.carpet(color).asItem()) && output.getCount() == 1, "Incorrect carpet crafting output");
                helper.assertTrue(holder.value().getRemainingItems(input).stream().allMatch(ItemStack::isEmpty)
                        && input.items().stream().allMatch(stack -> stack.getCount() == 1), "Carpet crafting duplicated or mutated ingredients");
                helper.assertTrue(!holder.value().canCraftInDimensions(2, 2), "Carpet recipe fitted a two-column crafting grid");
            }
        }
        helper.succeed();
    }

    /// Rejects mixed colors, wrong fish types, vertical arrangements, and incorrect quantities.
    @GameTest(template = "empty")
    public static void carpetRecipesRejectMixedColorsAndWrongInputs(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (var color : colors()) {
            for (var other : colors()) {
                if (other == color) continue;
                var input = CraftingInput.of(3, 1, List.of(fish(color), fish(other), fish(color)));
                helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(), "Mixed carpet colors were accepted");
            }
        }
        for (var item : List.of(Items.COD, Items.PUFFERFISH_BUCKET, PufferfishItems.NETHERITE.get(), PufferfishItems.GOLDEN.get())) {
            var input = CraftingInput.of(3, 1, Collections.nCopies(3, new ItemStack(item)));
            helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(), "Carpet recipe accepted an invalid fish type");
        }
        for (var input : List.of(
                CraftingInput.of(1, 3, Collections.nCopies(3, fish(null))),
                CraftingInput.of(2, 1, Collections.nCopies(2, fish(null))),
                CraftingInput.of(1, 1, List.of(new ItemStack(Items.PUFFERFISH, 3))))) {
            helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(), "Carpet recipe accepted an invalid arrangement");
        }
        helper.succeed();
    }

    /// Verifies vanilla carpet thickness and support behavior while preserving the dropped carpet's color.
    @GameTest(template = "empty")
    public static void carpetsPlaceAndDropWithTheirOriginalColor(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var base = helper.absolutePos(new BlockPos(1, 1, 1));
        var pos = base.above();
        for (var color : colors()) {
            var block = PufferfishBlocks.carpet(color);
            for (boolean removeSupport : new boolean[] {false, true}) {
                level.setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
                var stack = new ItemStack(block);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var hit = new BlockHitResult(Vec3.atCenterOf(base).add(0, 0.5, 0), Direction.UP, base, false);
                var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
                helper.assertTrue(((BlockItem) block.asItem()).place(context).consumesAction() && stack.isEmpty()
                        && level.getBlockState(pos).is(block), "Carpet placement failed");
                helper.assertTrue(level.getBlockState(pos).getShape(level, pos).max(Direction.Axis.Y) == 1.0 / 16,
                        "Carpet did not have vanilla carpet thickness");
                if (removeSupport) level.setBlockAndUpdate(base, Blocks.AIR.defaultBlockState());
                else level.destroyBlock(pos, true, player);
                helper.assertTrue(level.isEmptyBlock(pos), "Carpet remained after breaking or support removal");
                var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
                helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(block.asItem())
                        && drops.getFirst().getItem().getCount() == 1, "Carpet dropped the wrong color or quantity");
                drops.forEach(ItemEntity::discard);
            }
        }
        helper.succeed();
    }
}
