// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.glavo.mcmod.pufferfish.client.PlacedPufferfishRenderer;
import org.glavo.mcmod.pufferfish.test.RenderCapture;
import org.glavo.mcmod.pufferfish.test.TestItems;

public final class PufferfishPlacementClientTest {
    public static void verify(Minecraft minecraft) {
        verifyShapeMatchesModel(minecraft);
        for (var direction : Direction.Plane.HORIZONTAL) {
            var state = PufferfishBlocks.PLACED_FISH.get().defaultBlockState().setValue(PlacedPufferfishBlock.FACING, direction);
            var entity = new PlacedPufferfishBlockEntity(BlockPos.ZERO, state);
            var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(entity);
            if (!(renderer instanceof PlacedPufferfishRenderer)
                    || minecraft.getBlockRenderer().getBlockModel(state) == minecraft.getModelManager().getMissingModel()) {
                throw new IllegalStateException("Placed fish renderer or block model is missing");
            }
            for (var stack : TestItems.fishVariants()) {
                entity.setFish(stack);
                var pose = new PoseStack();
                var capture = new RenderCapture();
                renderer.render(entity, 0, pose, capture, 15728880, OverlayTexture.NO_OVERLAY);
                if (capture.count() == 0 || !pose.clear()) throw new IllegalStateException("Placed fish did not render: " + stack);
            }
        }
    }

    private static void verifyShapeMatchesModel(Minecraft minecraft) {
        var renderer = new PlacedPufferfishRenderer(minecraft.getEntityModels());
        for (var rank : PufferfishLevel.values()) for (int stage = 0; stage < 4; stage++) {
            var fish = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PUFFERFISH);
            PufferfishDurability.activate(fish);
            fish.setDamageValue((fish.getMaxDamage() * stage + 3) / 4);
            if (rank != PufferfishLevel.LEVEL_0) fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(rank, 0.5, 0, 0));
            for (var facing : Direction.Plane.HORIZONTAL) {
                var state = PufferfishBlocks.PLACED_FISH.get().defaultBlockState().setValue(PlacedPufferfishBlock.FACING, facing);
                var capture = new RenderCapture();
                renderer.render(fish, state, new PoseStack(), capture, 15728880, OverlayTexture.NO_OVERLAY);
                var rendered = capture.bounds();
                var shape = PufferfishSize.placedShape(fish, facing).bounds();
                if (!shape.inflate(0.001).contains(rendered.getMinPosition())
                        || !shape.inflate(0.001).contains(rendered.getMaxPosition())
                        || shape.getXsize() - rendered.getXsize() > 0.1
                        || shape.getYsize() - rendered.getYsize() > 0.1
                        || shape.getZsize() - rendered.getZsize() > 0.1) {
                    throw new IllegalStateException("Decoration shape does not match rendered fish: " + rank + "/" + stage + "/" + facing
                            + "; shape=" + shape + "; rendered=" + rendered);
                }
            }
        }
    }
}
