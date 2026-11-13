// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/// Verifies color-specific skin recipes, placement, shape, and drops.
@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishSkinGameTests {
    @GameTest(template = "empty", batch = "skin_rank")
    public static void rankedSkinsStackAndSurvivePlacement(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = helper.absolutePos(new BlockPos(1, 1, 1));
        var pos = base.above();
        level.setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var variant : PufferfishVariant.ALL) {
            var first = PufferfishSkinBlock.inheritLevel(org.glavo.mcmod.pufferfish.test.CuttingAssertions.rankedFish(variant, 0), new ItemStack(variant.skin()));
            var second = PufferfishSkinBlock.inheritLevel(org.glavo.mcmod.pufferfish.test.CuttingAssertions.rankedFish(variant, 1), new ItemStack(variant.skin()));
            player.getInventory().clearContent();
            helper.assertTrue(player.getInventory().add(first) && player.getInventory().add(second)
                    && player.getInventory().getItem(0).getCount() == 2 && player.getInventory().getItem(1).isEmpty(),
                    "Different individuals of the same rank did not stack as skins");
            for (var rank : List.of(PufferfishLevel.LEVEL_0, PufferfishLevel.LEVEL_1, PufferfishLevel.LEVEL_5, PufferfishLevel.LEVEL_10)) {
                var expected = new ItemStack(variant.skin());
                if (rank != PufferfishLevel.LEVEL_0) expected.set(PufferfishPlease.SKIN_LEVEL.get(), rank);
                var decoded = ItemStack.parse(level.registryAccess(), expected.save(level.registryAccess())).orElseThrow();
                helper.assertTrue(ItemStack.matches(expected, decoded), "Skin rank did not survive item serialization");
                if (rank != PufferfishLevel.LEVEL_0) helper.assertTrue(rank.displayName().equals(PufferfishCultivation.visibleName(decoded)),
                        "Skin tooltip displayed individual experience or lost its rank");
                player.setItemInHand(InteractionHand.MAIN_HAND, decoded);
                var hit = new BlockHitResult(Vec3.atCenterOf(base).add(0, 0.5, 0), Direction.UP, base, false);
                helper.assertTrue(((BlockItem) decoded.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, decoded, hit)).consumesAction(),
                        "Ranked skin placement failed");
                var state = level.getBlockState(pos);
                helper.assertTrue(state.getValue(PufferfishSkinBlock.LEVEL) == rank.number()
                        && ItemStack.matches(expected, variant.skin().getCloneItemStack(level, pos, state)), "Placed skin or pick-block lost rank");
                level.destroyBlock(pos, true, player);
                var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
                helper.assertTrue(drops.size() == 1 && ItemStack.matches(expected, drops.getFirst().getItem()), "Breaking a skin lost rank or added individual data");
                drops.forEach(ItemEntity::discard);
            }
        }
        helper.succeed();
    }
    private PufferfishSkinGameTests() {}

    /// Returns the undyed variant followed by all dye colors.
    private static List<DyeColor> colors() {
        var colors = new ArrayList<DyeColor>();
        colors.add(null);
        colors.addAll(Arrays.asList(DyeColor.values()));
        return colors;
    }

    /// Verifies all colors and orientations attach, occupy the correct thin volume, and drop once.
    @GameTest(template = "empty")
    public static void skinsAttachRotateAndDropWithTheirOriginalColor(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var base = helper.absolutePos(new BlockPos(2, 2, 2));
        var skins = new ArrayList<PufferfishSkinBlock>();
        for (var color : colors()) skins.add(PufferfishBlocks.skin(color));
        skins.add(PufferfishBlocks.GOLDEN_SKIN.get());
        skins.add(PufferfishBlocks.NETHERITE_SKIN.get());
        skins.add(PufferfishBlocks.HEAVY_SKIN.get());
        skins.add(PufferfishBlocks.COPPER_SKIN.get());
        skins.add(PufferfishBlocks.IRON_SKIN.get());
        skins.add(PufferfishBlocks.DIAMOND_SKIN.get());
        for (var block : skins) {
            for (var normal : Direction.values()) {
                var pos = base.relative(normal);
                for (var facing : Direction.Plane.HORIZONTAL) {
                    if (normal.getAxis() != Direction.Axis.Y && facing != normal) continue;
                    player.setYRot(facing.toYRot());
                    for (boolean removeSupport : new boolean[] {false, true}) {
                        level.setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
                        var stack = new ItemStack(block);
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        var hit = new BlockHitResult(Vec3.atCenterOf(base).add(Vec3.atLowerCornerOf(normal.getNormal()).scale(0.5)),
                                normal, base, false);
                        var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
                        helper.assertTrue(((BlockItem) block.asItem()).place(context).consumesAction() && stack.isEmpty()
                                && level.getBlockState(pos).is(block), "Skin placement failed: " + normal);
                        var state = level.getBlockState(pos);
                        var face = normal == Direction.UP ? AttachFace.FLOOR
                                : normal == Direction.DOWN ? AttachFace.CEILING : AttachFace.WALL;
                        helper.assertTrue(state.getValue(PufferfishSkinBlock.FACE) == face
                                && state.getValue(PufferfishSkinBlock.FACING) == facing, "Skin orientation ignored the placement context");
                        var shape = state.getShape(level, pos);
                        var collision = state.getCollisionShape(level, pos);
                        for (var axis : Direction.Axis.values()) {
                            double min = axis == normal.getAxis() && normal.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 15.0 / 16 : 0;
                            double max = axis == normal.getAxis() && normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 / 16 : 1;
                            helper.assertTrue(shape.min(axis) == min && shape.max(axis) == max
                                    && collision.min(axis) == min && collision.max(axis) == max, "Skin shape does not follow its surface");
                        }
                        helper.assertTrue(state.rotate(Rotation.CLOCKWISE_90).getValue(PufferfishSkinBlock.FACING) == facing.getClockWise()
                                && state.mirror(Mirror.LEFT_RIGHT).getValue(PufferfishSkinBlock.FACING) == Mirror.LEFT_RIGHT.mirror(facing),
                                "Skin structure rotation or mirroring failed");
                        if (removeSupport) level.setBlockAndUpdate(base, Blocks.AIR.defaultBlockState());
                        else level.destroyBlock(pos, true, player);
                        helper.assertTrue(level.isEmptyBlock(pos), "Skin remained after breaking or support removal");
                        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
                        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(block.asItem())
                                && drops.getFirst().getItem().getCount() == 1, "Skin dropped the wrong color or quantity");
                        drops.forEach(ItemEntity::discard);
                    }
                }
            }
        }
        helper.succeed();
    }

    /// Verifies floor support and prevents attachment to non-sturdy walls and ceilings.
    @GameTest(template = "empty")
    public static void skinsRequireSupportOnTheirAttachedSurface(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var base = helper.absolutePos(new BlockPos(2, 2, 2));
        var block = PufferfishBlocks.skin(null);
        helper.assertTrue(block.defaultBlockState().getValue(PufferfishSkinBlock.FACE) == AttachFace.FLOOR
                && block.defaultBlockState().getValue(PufferfishSkinBlock.FACING) == Direction.NORTH,
                "Skins must default to a north-facing floor orientation");
        level.setBlockAndUpdate(base, Blocks.OAK_SLAB.defaultBlockState());
        helper.assertTrue(block.defaultBlockState().canSurvive(level, base.above()), "Floor skin rejected a non-air support block");
        for (var normal : Direction.values()) {
            if (normal == Direction.UP) continue;
            level.setBlockAndUpdate(base, normal == Direction.DOWN
                    ? Blocks.OAK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                            net.minecraft.world.level.block.state.properties.SlabType.TOP)
                    : Blocks.OAK_SLAB.defaultBlockState());
            var stack = new ItemStack(block);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atCenterOf(base), normal, base, false);
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
            helper.assertTrue(block.getStateForPlacement(context) == null,
                    "Skin attached to an unsupported face or silently selected another surface");
        }
        helper.succeed();
    }
}
