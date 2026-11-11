// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.client.MaterialPufferfishRenderer;
import org.joml.Matrix4f;

public final class PufferfishMobClientTest {
    private PufferfishMobClientTest() {}

    public static void verify(Minecraft minecraft) {
        var target = new TextureTarget(1120, 400, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1120, 400, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        java.util.function.Consumer<net.neoforged.neoforge.client.event.RenderNameTagEvent> hideNames = event ->
                event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(hideNames);
        try {
            var level = PufferfishHeadgearClientTest.level(minecraft);
            var buffers = minecraft.renderBuffers().bufferSource();
            int index = 0;
            for (var variant : PufferfishMobs.VARIANTS.values()) {
                int x = index % 7 * 160, y = index / 7 * 200;
                for (int item = 0; item < 2; item++) {
                    var stack = new ItemStack(item == 0 ? variant.bucket().get() : variant.egg().get());
                    var model = minecraft.getItemRenderer().getModel(stack, level, null, 0);
                    if (model == minecraft.getModelManager().getMissingModel() || stack.getHoverName().getString().startsWith("item.")) {
                        throw new IllegalStateException("Missing material fish bucket or spawn egg resources: " + stack);
                    }
                    Lighting.setupForFlatItems();
                    var pose = new PoseStack();
                    pose.translate(x + 45 + item * 70, y + 40, 0);
                    pose.scale(40, -40, 40);
                    minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, 15728880, OverlayTexture.NO_OVERLAY, pose, buffers, level, 0);
                    buffers.endBatch();
                }
                var fish = variant.entity().get().create(level);
                var renderer = minecraft.getEntityRenderDispatcher().getRenderer(fish);
                if (!(renderer instanceof MaterialPufferfishRenderer material)) throw new IllegalStateException("Missing material fish entity renderer");
                if (!material.getTextureLocation(fish).getNamespace().equals(PufferfishPlease.MODID)) {
                    throw new IllegalStateException("Material fish used the vanilla body texture");
                }
                Lighting.setupFor3DItems();
                for (int puff = 0; puff < 3; puff++) {
                    fish.setPuffState(puff);
                    if (PufferfishItems.isRadiant(fish.fish()) && puff == 2) {
                        fish.setAttackPhase(org.glavo.mcmod.pufferfish.entity.MaterialPufferfish.PULSE);
                    }
                    var pose = new PoseStack();
                    pose.translate(x + 27 + puff * 53, y + 145, 0);
                    pose.scale(60, -60, 60);
                    pose.mulPose(Axis.YP.rotationDegrees(25));
                    renderer.render(fish, 0, 0, pose, buffers, 15728880);
                    buffers.endBatch();
                }
                index++;
            }
            try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(Path.of("pufferfish-mobs-preview.png")); }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save material fish entity preview", exception);
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(hideNames);
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
