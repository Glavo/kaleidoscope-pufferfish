// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.glavo.mcmod.pufferfish.client.PlacedPufferfishRenderer;
import org.glavo.mcmod.pufferfish.test.RenderTestScene;
import org.glavo.mcmod.pufferfish.test.TestImages;
import org.glavo.mcmod.pufferfish.test.TestItems;

public final class PufferfishPlacementClientTest {
    public static void verify(Minecraft minecraft) {
        try (var scene = new RenderTestScene(minecraft, 900, 600, 0.08F, 0.14F, 0.2F)) {
            var target = scene.target();
            Lighting.setupFor3DItems();

            var state = PufferfishBlocks.PLACED_FISH.get().defaultBlockState().setValue(PlacedPufferfishBlock.FACING, Direction.SOUTH);
            var entity = new PlacedPufferfishBlockEntity(BlockPos.ZERO, state);
            var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(entity);
            if (!(renderer instanceof PlacedPufferfishRenderer)
                    || minecraft.getBlockRenderer().getBlockModel(state) == minecraft.getModelManager().getMissingModel()) {
                throw new IllegalStateException("Placed fish renderer or block model is missing");
            }
            var variants = TestItems.fishVariants();
            var buffers = minecraft.renderBuffers().bufferSource();
            for (int i = 0; i < variants.size(); i++) {
                entity.setFish(variants.get(i));
                var pose = new PoseStack();
                pose.translate(75 + i % 6 * 150, 100 + i / 6 * 150, 0);
                pose.scale(130, -130, 130);
                pose.mulPose(Axis.XP.rotationDegrees(20));
                pose.mulPose(Axis.YP.rotationDegrees(30));
                pose.translate(-0.5, 0, -0.5);
                renderer.render(entity, 0, pose, buffers, 15728880, OverlayTexture.NO_OVERLAY);
                buffers.endBatch();
            }
            TestImages.capture(target, Path.of("placed-pufferfish-preview.png"));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save placed fish preview", exception);
        }
    }
}
