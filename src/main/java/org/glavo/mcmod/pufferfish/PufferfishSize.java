// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

public final class PufferfishSize {
    private static final net.minecraft.resources.ResourceLocation MODIFIER = PufferfishPlease.id("cultivation_scale");

    private PufferfishSize() {}

    public static float scale(ItemStack stack) {
        var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
        return cultivation == null ? 1 : cultivation.level().scale();
    }

    public static float equipmentScale(ItemStack stack) { return Math.min(scale(stack), 1.25F); }

    /// Lowest rendered vertex relative to the support plane, independent of rank.
    public static float floorOffset(int stage) {
        return switch (stage) {
            case 1 -> (1 - (float) Math.sqrt(2) * 1.3F) / 16;
            case 2 -> (1 - (float) Math.sqrt(2)) / 16;
            case 3 -> 1.0F / 16;
            default -> 0;
        };
    }

    public static VoxelShape placedShape(ItemStack stack, Direction facing) {
        var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
        int rank = cultivation == null ? 0 : cultivation.level().number();
        return PlacedShapes.SHAPES[rank][PufferfishDurability.deflationStage(stack)][facing.get2DDataValue()];
    }

    private static final class PlacedShapes {
        private static final VoxelShape[][][] SHAPES = create();

        private static VoxelShape[][][] create() {
            var shapes = new VoxelShape[PufferfishLevel.values().length][4][4];
            for (var rank : PufferfishLevel.values()) for (int stage = 0; stage < 4; stage++) {
                double modelScale = stage == 1 ? 1.3 : 1;
                double width = stage == 0 ? 6 : stage == 3 ? 2.5 : 4.5 * modelScale;
                double front = stage == 0 ? 5 : stage == 3 ? 1.5 : (2.5 + Math.sqrt(0.5)) * modelScale;
                double back = stage == 0 ? 5 : stage == 3 ? 4.5 : (2.5 + Math.sqrt(2)) * modelScale;
                double height = stage == 0 ? 10 : stage == 3 ? 3 : (5 + 2 * Math.sqrt(2)) * modelScale;
                double scale = rank.scale() / 16.0;
                double bottom = floorOffset(stage);
                var bounds = new AABB(-width * scale, bottom, -front * scale,
                        width * scale, bottom + height * scale, back * scale);
                for (var facing : new Direction[] {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST}) {
                    shapes[rank.number()][stage][facing.get2DDataValue()] = Shapes.create(bounds.move(0.5, 0, 0.5));
                    bounds = new AABB(bounds.minZ, bounds.minY, -bounds.maxX, bounds.maxZ, bounds.maxY, -bounds.minX);
                }
            }
            return shapes;
        }
    }

    /// The native attribute synchronizes geometry and collision size without replacing external modifiers.
    public static void update(Pufferfish fish) {
        if (fish.level().isClientSide) return;
        var stack = PufferfishData.stored(fish);
        @Nullable var attribute = fish.getAttribute(Attributes.SCALE);
        if (attribute == null) return;
        double amount = scale(stack) - 1;
        @Nullable var previous = attribute.getModifier(MODIFIER);
        if (previous == null ? amount == 0 : previous.amount() == amount) return;
        attribute.removeModifier(MODIFIER);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(MODIFIER, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        fish.refreshDimensions();
    }

    public static void joinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Pufferfish fish) update(fish);
    }
}
