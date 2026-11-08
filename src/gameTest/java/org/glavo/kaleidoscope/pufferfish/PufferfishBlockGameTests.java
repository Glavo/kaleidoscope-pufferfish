// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishBlockGameTests {
    private static List<DyeColor> colors() {
        var colors = new ArrayList<DyeColor>();
        colors.add(null);
        colors.addAll(Arrays.asList(DyeColor.values()));
        return colors;
    }

    private static ItemStack fish(DyeColor color) {
        var stack = new ItemStack(Items.PUFFERFISH);
        return color == null ? stack : PufferfishColors.dye(stack, color);
    }

    @GameTest(template = "empty")
    public static void compressionAndUnpackingPreserveEveryColor(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (var color : colors()) {
            var original = fish(color);
            var input = CraftingInput.of(3, 3, Collections.nCopies(9, original));
            var holder = manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
            helper.assertTrue(holder.id().equals(KaleidoscopePufferfish.id(PufferfishBlocks.name(color))), "Wrong compression recipe");
            var packed = holder.value().assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(packed.is(PufferfishBlocks.get(color).asItem()) && packed.getCount() == 1, "Wrong compressed block");
            helper.assertTrue(holder.value().getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Compression duplicated items");
            var unpackInput = CraftingInput.of(1, 1, List.of(packed));
            var unpack = manager.getRecipeFor(RecipeType.CRAFTING, unpackInput, helper.getLevel()).orElseThrow().value();
            var restored = unpack.assemble(unpackInput, helper.getLevel().registryAccess());
            helper.assertTrue(restored.getCount() == 9 && ItemStack.isSameItemSameComponents(original, restored), "Unpacking lost color or changed quantity");
            helper.assertTrue(unpack.getRemainingItems(unpackInput).stream().allMatch(ItemStack::isEmpty), "Unpacking duplicated items");
            helper.assertTrue(original.getCount() == 1 && packed.getCount() == 1, "Recipe mutated inputs");
            for (var wrongColor : colors()) {
                if (wrongColor != color) {
                    var mixed = new ArrayList<>(Collections.nCopies(9, original));
                    mixed.set(4, fish(wrongColor));
                    helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, mixed), helper.getLevel()).isEmpty(), "Mixed colors were accepted");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void compressionRejectsIncompleteGridsAndBuckets(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (var color : colors()) {
            var stack = fish(color);
            for (int missing = 0; missing < 9; missing++) {
                var grid = new ArrayList<>(Collections.nCopies(9, stack));
                grid.set(missing, ItemStack.EMPTY);
                helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), helper.getLevel()).isEmpty(), "Incomplete grid was accepted");
            }
            var bucket = new ItemStack(Items.PUFFERFISH_BUCKET);
            if (color != null) {
                PufferfishColors.dye(bucket, color);
            }
            for (var input : List.of(
                    CraftingInput.of(1, 1, List.of(stack.copyWithCount(9))),
                    CraftingInput.of(2, 2, Collections.nCopies(4, stack)),
                    CraftingInput.of(3, 3, Collections.nCopies(9, bucket)),
                    CraftingInput.of(3, 3, Collections.nCopies(9, new ItemStack(Items.COD))))) {
                helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(), "Invalid compression input was accepted");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ingredientNetworkSyncPreservesColorMatching(GameTestHelper helper) {
        for (var color : colors()) {
            Ingredient original = new PufferfishIngredient(Optional.ofNullable(color)).toVanilla();
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, original);
                var decoded = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                helper.assertTrue(!decoded.isSimple() && decoded.getItems().length == 1, "Ingredient lost component matching or display stack");
                helper.assertTrue(ItemStack.isSameItemSameComponents(decoded.getItems()[0], fish(color)), "Ingredient display lost color");
                for (var candidate : colors()) {
                    helper.assertTrue(decoded.test(fish(candidate)) == (candidate == color), "Synced ingredient accepted the wrong color");
                }
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placedBlocksDropTheirOriginalColor(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getAbilities().mayBuild = true;
        var base = helper.absolutePos(new BlockPos(1, 1, 1));
        var pos = base.above();
        level.setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
        for (var color : colors()) {
            var block = PufferfishBlocks.get(color);
            var stack = new ItemStack(block);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atCenterOf(base).add(0, 0.5, 0), Direction.UP, base, false);
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
            helper.assertTrue(((BlockItem) block.asItem()).place(context).consumesAction(), "Block placement failed");
            helper.assertTrue(level.getBlockState(pos).is(block) && stack.isEmpty(), "Placed block or consumption was incorrect");
            helper.assertTrue(level.destroyBlock(pos, true, player), "Block destruction failed");
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(block.asItem())
                    && drops.getFirst().getItem().getCount() == 1, "Broken block lost its color or duplicated drops");
            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }
}
