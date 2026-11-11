// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.mcmod.pufferfish.client.PufferfishConduitRenderer;
import org.joml.Matrix4f;

public final class PufferfishConduitClientTest {
    private static final int LIGHT = 15728880;
    private static final RenderType FISH = RenderType.entityCutoutNoCull(ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png"));

    public static void verify(Minecraft minecraft) {
        var state = PufferfishBlocks.CONDUIT.get().defaultBlockState();
        var entity = new PufferfishConduitBlockEntity(BlockPos.ZERO, state);
        var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof PufferfishConduitRenderer)
                || !(minecraft.getBlockEntityRenderDispatcher().getRenderer(new ConduitBlockEntity(BlockPos.ZERO, Blocks.CONDUIT.defaultBlockState())) instanceof ConduitRenderer)) {
            throw new IllegalStateException("Pufferfish conduit renderer is missing or replaced vanilla conduit rendering");
        }
        var stack = new ItemStack(PufferfishBlocks.CONDUIT.get());
        var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
        if (!model.isCustomRenderer() || model == minecraft.getModelManager().getMissingModel()
                || stack.getHoverName().getString().startsWith("block.")) {
            throw new IllegalStateException("Pufferfish conduit item model or translation is missing");
        }
        for (boolean active : new boolean[] {false, true}) {
            active(entity, active);
            entity.tickCount = 33;
            var capture = new Capture();
            var pose = new PoseStack();
            renderer.render(entity, 0, pose, capture, LIGHT, OverlayTexture.NO_OVERLAY);
            var fish = capture.parts.get(FISH);
            if (fish == null || fish.count < 100 || fish.minY < 0.05F || fish.maxY > 0.95F || !pose.clear()
                    || capture.parts.size() != (active ? 2 : 1)) {
                throw new IllegalStateException("Pufferfish conduit fish or active wind failed to render");
            }
        }
        for (var context : ItemDisplayContext.values()) {
            var capture = new Capture();
            var pose = new PoseStack();
            IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, context, pose, capture, LIGHT, OverlayTexture.NO_OVERLAY);
            if (!capture.parts.containsKey(FISH) || !pose.clear()) throw new IllegalStateException("Conduit item did not render: " + context);
        }
        preview(minecraft, entity);
    }

    private static void active(PufferfishConduitBlockEntity entity, boolean active) {
        // The server tests cover activation; render fixtures need both states without a running world.
        try {
            var field = ConduitBlockEntity.class.getDeclaredField("isActive");
            field.setAccessible(true);
            field.setBoolean(entity, active);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not prepare conduit render fixture", exception);
        }
    }

    private static void preview(Minecraft minecraft, PufferfishConduitBlockEntity entity) {
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
            var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(entity);
            var buffers = minecraft.renderBuffers().bufferSource();
            for (int i = 0; i < 2; i++) {
                active(entity, i == 1);
                entity.tickCount = 24;
                var pose = new PoseStack();
                pose.translate(230 + i * 440, 220, 0);
                pose.scale(270, -270, 270);
                pose.mulPose(Axis.XP.rotationDegrees(20));
                pose.mulPose(Axis.YP.rotationDegrees(35));
                pose.translate(-0.5, -0.5, -0.5);
                renderer.render(entity, 0, pose, buffers, LIGHT, OverlayTexture.NO_OVERLAY);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-conduit-preview.png"));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save conduit preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static final class Capture implements MultiBufferSource {
        private final Map<RenderType, Vertices> parts = new HashMap<>();
        @Override
        public VertexConsumer getBuffer(RenderType type) { return parts.computeIfAbsent(type, key -> new Vertices()); }
    }

    private static final class Vertices implements VertexConsumer {
        private int count;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            count++;
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
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
