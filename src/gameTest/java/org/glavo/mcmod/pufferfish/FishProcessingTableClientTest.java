// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
import org.glavo.mcmod.pufferfish.test.RenderCapture;
import org.glavo.mcmod.pufferfish.test.RenderTestScene;

public final class FishProcessingTableClientTest {
    public static void verify(Minecraft minecraft) {
        var level = ClientTestLevel.level(minecraft);
        var state = PufferfishBlocks.PROCESSING_TABLE.get().defaultBlockState().setValue(FishProcessingTableBlock.FACING, net.minecraft.core.Direction.SOUTH);
        var table = new FishProcessingTableBlockEntity(BlockPos.ZERO, state);
        table.setLevel(level);
        var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(table);
        if (!(renderer instanceof org.glavo.mcmod.pufferfish.client.FishProcessingTableRenderer)) throw new IllegalStateException("Missing processing table renderer");
        for (var stack : java.util.List.of(PufferfishVariant.NORMAL.fish(), PufferfishVariant.RADIANT.fish(), new ItemStack(PufferfishBlocks.HEAVY_SKIN.get()))) {
            table.setLevel(null);
            table.removeItem();
            table.insert(stack);
            table.setLevel(level);
            table.triggerEvent(1, 1);
            var pose = new PoseStack();
            var capture = new RenderCapture();
            renderer.render(table, 4, pose, capture, 15728880, OverlayTexture.NO_OVERLAY);
            if (capture.count() == 0 || !pose.clear()) throw new IllegalStateException("Table input geometry is missing or leaked a transform");
        }
        var emptyStand = new FishProcessingTableBlockEntity(BlockPos.ZERO, state);
        emptyStand.setLevel(level);
        table.onDataPacket(ClientTestLevel.connection(minecraft).getConnection(), emptyStand.getUpdatePacket(), level.registryAccess());
        var clearedPose = new PoseStack();
        var clearedGeometry = new RenderCapture();
        renderer.render(table, 0, clearedPose, clearedGeometry, 15728880, OverlayTexture.NO_OVERLAY);
        if (!table.item().isEmpty() || clearedGeometry.count() != 0 || !clearedPose.clear()) {
            throw new IllegalStateException("Cleared stand still renders its previous input after a network update");
        }
        table.setLevel(null);
        table.removeItem();
        table.insert(PufferfishVariant.NORMAL.fish());
        table.setLevel(level);
        try (var scene = new RenderTestScene(minecraft, 384, 384, 0.12F, 0.16F, 0.2F)) {
            var pose = new PoseStack();
            pose.translate(192, 240, 0);
            pose.scale(210, -210, 210);
            pose.mulPose(Axis.XP.rotationDegrees(25));
            pose.mulPose(Axis.YP.rotationDegrees(35));
            pose.translate(-0.5, -0.3, -0.5);
            var buffers = minecraft.renderBuffers().bufferSource();
            minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers, 15728880, OverlayTexture.NO_OVERLAY);
            renderer.render(table, 8, pose, buffers, 15728880, OverlayTexture.NO_OVERLAY);
            buffers.endBatch();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(scene.target())) {
                var path = minecraft.gameDirectory.toPath().resolve("screenshots/fish-processing-table.png");
                java.nio.file.Files.createDirectories(path.getParent());
                image.writeToFile(path);
            } catch (java.io.IOException exception) { throw new IllegalStateException("Could not save processing table preview", exception); }
        }
    }
}
