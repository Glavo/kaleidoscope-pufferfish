// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishLampGameTests {
    @GameTest(template = "empty")
    public static void everyFishCraftsOnlyItsMatchingLamp(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(PufferfishLamps.VARIANTS.size() == 30, "Missing lamp variants");
        for (var variant : PufferfishLamps.VARIANTS.values()) {
            var fish = variant.fish().get();
            for (boolean reversed : new boolean[] {false, true}) {
                var torch = new ItemStack(Items.TORCH);
                var input = CraftingInput.of(2, 1, reversed ? List.of(torch, fish) : List.of(fish, torch));
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().value();
                var result = recipe.assemble(input, level.registryAccess());
                helper.assertTrue(result.is(variant.block().get().asItem()) && result.getCount() == 1,
                        "Lamp recipe lost the fish variant");
                helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Lamp recipe returned ingredients");
            }
        }
        for (var invalid : List.of(new ItemStack(Items.PUFFERFISH_BUCKET), new ItemStack(Items.COD),
                new ItemStack(PufferfishItems.FLAIL.get()), new ItemStack(PufferfishItems.SASHIMI.get()))) {
            helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                    CraftingInput.of(2, 1, List.of(new ItemStack(Items.TORCH), invalid)), level).isEmpty(), "Lamp accepted an invalid fish");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lampsPlaceUnderwaterSaveAndDropTheirVariant(GameTestHelper helper) {
        var level = helper.getLevel();
        var relative = new BlockPos(2, 2, 2);
        var pos = helper.absolutePos(relative);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(90);
        for (var variant : PufferfishLamps.VARIANTS.values()) {
            helper.setBlock(relative.below(), Blocks.STONE);
            helper.setBlock(relative, Blocks.WATER);
            var stack = new ItemStack(variant.block().get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
            helper.assertTrue(((BlockItem) stack.getItem()).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Lamp placement failed");
            var state = level.getBlockState(pos);
            helper.assertTrue(state.is(variant.block().get()) && state.getLightEmission(level, pos) == 15
                    && state.getFluidState().getType() == Fluids.WATER && stack.getCount() == 1, "Lamp placement or light was incorrect");
            var entity = (PlacedPufferfishBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(ItemStack.isSameItemSameComponents(entity.fish(), variant.fish().get()), "Lamp displays the wrong fish");
            var saved = entity.saveWithFullMetadata(level.registryAccess());
            var restored = (PlacedPufferfishBlockEntity) BlockEntity.loadStatic(pos, state, saved, level.registryAccess());
            helper.assertTrue(restored != null && ItemStack.matches(entity.fish(), restored.fish()), "Lamp lost its fish on reload");
            var drops = Block.getDrops(state, level, pos, entity);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(stack.getItem())
                    && state.getBlock().getCloneItemStack(level, pos, state).is(stack.getItem()), "Lamp dropped or cloned a raw fish");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void copperLampWaxingPreservesLightAndWater(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos, PufferfishLamps.VARIANTS.get("oxidized_copper_pufferfish_lamp").block().get().defaultBlockState()
                .setValue(PlacedPufferfishBlock.WATERLOGGED, true).setValue(PlacedPufferfishBlock.FACING, Direction.EAST));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var item : List.of(Items.HONEYCOMB, Items.IRON_AXE, Items.IRON_AXE)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
            var state = level.getBlockState(pos);
            var entity = (PlacedPufferfishBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(state.getLightEmission(level, pos) == 15 && state.getValue(PlacedPufferfishBlock.WATERLOGGED)
                    && state.getValue(PlacedPufferfishBlock.FACING) == Direction.EAST, "Waxing changed lamp properties");
            helper.assertTrue(PufferfishLamps.lamp(entity.fish()).is(state.getBlock().asItem()), "Lamp block and fish variant disagree");
        }
        helper.assertTrue(level.getBlockState(pos).is(PufferfishLamps.VARIANTS.get("weathered_copper_pufferfish_lamp").block().get()), "Lamp scraping failed");
        helper.succeed();
    }
}
