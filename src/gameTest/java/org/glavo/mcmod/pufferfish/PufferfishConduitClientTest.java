// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
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

public final class PufferfishConduitClientTest {
    private static final int LIGHT = 15728880;
    private static final RenderType FISH = RenderType.entityCutoutNoCull(ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png"));

    public static void verify(Minecraft minecraft) {
        verifyFrameModels(minecraft);
        for (var block : java.util.List.of(PufferfishBlocks.CONDUIT.get(), PufferfishBlocks.ANCIENT_CONDUIT.get())) {
            var state = block.defaultBlockState();
            var entity = new PufferfishConduitBlockEntity(BlockPos.ZERO, state);
            var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(entity);
            if (!(renderer instanceof PufferfishConduitRenderer)
                    || !(minecraft.getBlockEntityRenderDispatcher().getRenderer(new ConduitBlockEntity(BlockPos.ZERO, Blocks.CONDUIT.defaultBlockState())) instanceof ConduitRenderer)) {
                throw new IllegalStateException("Pufferfish conduit renderer is missing or replaced vanilla conduit rendering");
            }
            var expectedTexture = block == PufferfishBlocks.ANCIENT_CONDUIT.get()
                    ? RenderType.entityCutoutNoCull(org.glavo.mcmod.pufferfish.client.PufferfishTextures.ancientConduit()) : FISH;
            var stack = new ItemStack(block);
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
                var fish = capture.parts.get(expectedTexture);
                if (fish == null || fish.count < 100 || fish.minY < 0.05F || fish.maxY > 0.95F || !pose.clear()
                        || capture.parts.size() != (active ? 2 : 1)) {
                    throw new IllegalStateException("Pufferfish conduit fish or active wind failed to render");
                }
            }
            for (var context : ItemDisplayContext.values()) {
                var capture = new Capture();
                var pose = new PoseStack();
                IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, context, pose, capture, LIGHT, OverlayTexture.NO_OVERLAY);
                if (!capture.parts.containsKey(expectedTexture) || !pose.clear()) throw new IllegalStateException("Conduit item did not render: " + context);
            }
        }
    }

    private static void verifyFrameModels(Minecraft minecraft) {
        var brushable = new AncientPrismarineBlockEntity(BlockPos.ZERO, PufferfishBlocks.SUSPICIOUS_ANCIENT_PRISMARINE.get().defaultBlockState());
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer<?> renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(brushable);
        if (!(renderer instanceof net.minecraft.client.renderer.blockentity.BrushableBlockRenderer)) {
            throw new IllegalStateException("Archaeology renderer is missing");
        }
        var hashes = new java.util.HashSet<Integer>();
        for (int stage = -1; stage < 4; stage++) {
            var state = stage < 0 ? PufferfishBlocks.ANCIENT_PRISMARINE.get().defaultBlockState()
                    : brushable.getBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DUSTED, stage);
            var model = minecraft.getBlockRenderer().getBlockModel(state);
            var expected = PufferfishPlease.id("block/" + (stage < 0 ? "ancient_prismarine" : "suspicious_ancient_prismarine_" + stage));
            if (model == minecraft.getModelManager().getMissingModel() || !model.getParticleIcon().contents().name().equals(expected)) {
                throw new IllegalStateException("Missing ancient prismarine model or texture");
            }
            var pixels = model.getParticleIcon().contents().getOriginalImage();
            int hash = 1;
            for (int y = 0; y < pixels.getHeight(); y++) for (int x = 0; x < pixels.getWidth(); x++) hash = hash * 31 + pixels.getPixelRGBA(x, y);
            if (!hashes.add(hash)) throw new IllegalStateException("Archaeology stages have indistinguishable textures");
        }
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
