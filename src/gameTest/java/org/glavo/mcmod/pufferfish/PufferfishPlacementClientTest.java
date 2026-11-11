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
}
