// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;

/// Checks flail item geometry and resource reloads.
public final class PufferfishFlailClientTest {
    /// Full-bright lighting used by geometry checks and previews.
    private static final int LIGHT = 15728880;

    /// Prevents construction of the test holder.
    private PufferfishFlailClientTest() {}

    /// Verifies both variants in every display context and saves a three-variant preview.
    public static void verify(Minecraft minecraft) {
        for (var item : new PufferfishFlailItem[] {PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get()}) {
            var stack = new ItemStack(item);
            stack.set(KaleidoscopePufferfish.COLOR.get(), DyeColor.RED);
            var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
            if (!model.isCustomRenderer() || model == minecraft.getModelManager().getMissingModel()
                    || stack.getHoverName().getString().startsWith("item.")) {
                throw new IllegalStateException("Missing flail model or translation");
            }
            for (var context : ItemDisplayContext.values()) {
                var pose = new PoseStack();
                var capture = new Capture();
                IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, context, pose, capture, LIGHT, OverlayTexture.NO_OVERLAY);
                if (capture.vertices < 300 || capture.types.size() != 2 || !pose.clear()) {
                    throw new IllegalStateException("Flail head or chain failed to render: " + context);
                }
            }
        }
        preview(minecraft);
    }

    /// Saves ordinary, dyed, and netherite flails using their actual GUI display transforms.
    private static void preview(Minecraft minecraft) {
        var target = new TextureTarget(900, 440, true, Minecraft.ON_OSX);
        target.setClearColor(0.08F, 0.14F, 0.2F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 900, 440, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var red = new ItemStack(PufferfishItems.FLAIL.get());
            red.set(KaleidoscopePufferfish.COLOR.get(), DyeColor.RED);
            var stacks = new ItemStack[] {new ItemStack(PufferfishItems.FLAIL.get()), red, new ItemStack(PufferfishItems.NETHERITE_FLAIL.get())};
            for (int i = 0; i < stacks.length; i++) {
                var pose = new PoseStack();
                pose.translate(150 + i * 300, 220, 0);
                pose.scale(260, -260, 260);
                var buffers = minecraft.renderBuffers().bufferSource();
                minecraft.getItemRenderer().renderStatic(stacks[i], ItemDisplayContext.GUI, LIGHT, OverlayTexture.NO_OVERLAY,
                        pose, buffers, null, 0);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-flail-preview.png"));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save flail preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    /// Counts submitted vertices and distinct render types without allocating GPU buffers.
    private static final class Capture implements MultiBufferSource, VertexConsumer {
        /// Render types observed during the current item render.
        private final Set<RenderType> types = new HashSet<>();
        /// Number of vertices emitted by the item renderer.
        private int vertices;

        /// Creates an empty geometry capture.
        private Capture() {}

        @Override
        public VertexConsumer getBuffer(RenderType type) { types.add(type); return this; }
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new IllegalStateException("Non-finite flail vertex");
            }
            vertices++;
            return this;
        }
        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        @Override
        public VertexConsumer setUv(float u, float v) { return this; }
        @Override
        public VertexConsumer setUv1(int u, int v) { return this; }
        @Override
        public VertexConsumer setUv2(int u, int v) { return this; }
        @Override
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
