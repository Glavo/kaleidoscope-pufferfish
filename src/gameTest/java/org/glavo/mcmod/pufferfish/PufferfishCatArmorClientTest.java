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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

public final class PufferfishCatArmorClientTest {
    public static void verify(Minecraft minecraft) {
        var target = new TextureTarget(1440, 1260, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1440, 1260, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
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
            for (var entry : PufferfishCatArmor.VARIANTS.entrySet()) {
                var stack = entry.getValue().toStack();
                var model = minecraft.getItemRenderer().getModel(stack, level, null, 0);
                if (model == minecraft.getModelManager().getMissingModel() || model.getQuads(null, null, RandomSource.create()).isEmpty()
                        || model.isGui3d()
                        || !model.getParticleIcon().contents().name().equals(PufferfishPlease.id("item/" + entry.getKey()))
                        || stack.getHoverName().getString().startsWith("item.")) {
                    throw new IllegalStateException("Missing cat armor item resources: " + entry.getKey());
                }
                int x = index % 6 * 240, y = index / 6 * 210;
                Lighting.setupForFlatItems();
                var pose = new PoseStack();
                pose.translate(x + 120, y + 35, 0);
                pose.scale(50, -50, 50);
                minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, 15728880, OverlayTexture.NO_OVERLAY, pose, buffers, level, 0);
                buffers.endBatch();
                Lighting.setupFor3DItems();
                for (int posture = 0; posture < 3; posture++) {
                    var cat = EntityType.CAT.create(level);
                    cat.setTame(true, false);
                    cat.setBodyArmorItem(stack.copy());
                    cat.setInSittingPose(posture == 1);
                    if (posture == 2) {
                        cat.setNoAi(true);
                        cat.setNoGravity(true);
                        cat.setSilent(true);
                        cat.setLying(true);
                        for (int tick = 0; tick < 15; tick++) cat.tick();
                        if (cat.getLieDownAmount(0) < 1) throw new IllegalStateException("Cat did not enter the lying pose");
                    }
                    if (index == 30) cat.setAge(-24000);
                    var renderer = minecraft.getEntityRenderDispatcher().getRenderer(cat);
                    pose = new PoseStack();
                    pose.translate(x + 40 + posture * 80, y + 180, 0);
                    pose.scale(75, -75, 75);
                    pose.mulPose(Axis.YP.rotationDegrees(35));
                    renderer.render(cat, 0, 0, pose, buffers, 15728880);
                    buffers.endBatch();
                }
                index++;
            }
            try (var image = Screenshot.takeScreenshot(target)) { image.writeToFile(Path.of("pufferfish-cat-armor-preview.png")); }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save cat armor preview", exception);
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
