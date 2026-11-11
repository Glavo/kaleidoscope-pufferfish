// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.glavo.mcmod.pufferfish.PufferfishColors;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.client.PufferfishTextures;
import org.glavo.mcmod.pufferfish.compat.create.client.CreateClientIntegration;
import org.glavo.mcmod.pufferfish.compat.create.client.PufferfishEngineRenderer;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;

public final class PufferfishEngineClientTest {
    public static void verify(Minecraft minecraft) {
        var missing = minecraft.getModelManager().getMissingModel();
        for (String part : CreateClientIntegration.PARTS) {
            var model = minecraft.getModelManager().getModel(CreateClientIntegration.model(part));
            if (model == missing || model.getQuads(null, null, RandomSource.create()).isEmpty()) {
                throw new IllegalStateException("Missing engine part: " + part);
            }
        }
        if (minecraft.getItemRenderer().getModel(new ItemStack(CreateIntegration.ENGINE_ITEM.get()), null, null, 0) == missing) {
            throw new IllegalStateException("Missing engine item model");
        }
        List<ItemStack> variants = new ArrayList<>();
        variants.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) {
            variants.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        }
        variants.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        variants.add(new ItemStack(PufferfishItems.GOLDEN.get()));
        variants.addAll(List.of(new ItemStack(PufferfishItems.COPPER.get()), new ItemStack(PufferfishItems.IRON.get()), new ItemStack(PufferfishItems.DIAMOND.get())));
        HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        for (var direction : Direction.Plane.HORIZONTAL) {
            var state = CreateIntegration.ENGINE.get().defaultBlockState().setValue(PufferfishEngineBlock.HORIZONTAL_FACING, direction);
            if (minecraft.getBlockRenderer().getBlockModel(state) == missing) {
                throw new IllegalStateException("Missing engine block model: " + direction);
            }
            for (var fish : variants) {
                var engine = new PufferfishEngineBlockEntity(BlockPos.ZERO, state);
                engine.setLevel(ClientTestLevel.level(minecraft));
                var tag = new CompoundTag();
                tag.put("Fish", fish.save(registries));
                tag.putBoolean("Water", true);
                tag.putInt("ConfiguredSpeed", 16);
                engine.loadWithComponents(tag, registries);
                var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(engine);
                if (!(renderer instanceof PufferfishEngineRenderer)) {
                    throw new IllegalStateException("Missing engine renderer");
                }
                var capture = new Capture();
                renderer.render(engine, 0, new PoseStack(), capture, 15728880, 0);
                var color = PufferfishColors.get(fish);
                var texture = PufferfishItems.isNetherite(fish) ? PufferfishTextures.netheriteEntity()
                        : PufferfishItems.materialName(fish) != null ? PufferfishTextures.materialEntity(PufferfishItems.materialName(fish))
                        : color == null ? ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png") : PufferfishTextures.entity(color);
                var fishVertices = capture.parts.get(RenderType.entityCutoutNoCull(texture));
                if (fishVertices == null || fishVertices.count < 100 || fishVertices.minY < 0.2 || fishVertices.maxY > 0.85
                        || !capture.parts.containsKey(RenderType.translucent())) {
                    throw new IllegalStateException("Engine fish or water rendering failed: " + fish + ", " + direction);
                }
                engine.setSpeed(16);
                for (int tick = 0; tick < 38; tick++) {
                    engine.tick();
                }
                var inflated = new Capture();
                renderer.render(engine, 1, new PoseStack(), inflated, 15728880, 0);
                var largeFish = inflated.parts.get(RenderType.entityCutoutNoCull(texture));
                if (largeFish.maxY - largeFish.minY < (fishVertices.maxY - fishVertices.minY) * 1.5F
                        || largeFish.minY < 0.05 || largeFish.maxY > 0.95) {
                    throw new IllegalStateException("Engine inflation animation failed or escaped its chamber");
                }
            }
        }
    }

    private static final class Capture implements MultiBufferSource {
        private final Map<RenderType, Vertices> parts = new HashMap<>();

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return parts.computeIfAbsent(type, key -> new Vertices());
        }
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
