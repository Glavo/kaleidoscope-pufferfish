// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.glavo.kaleidoscope.pufferfish.PufferfishDurability;

/// Preserves resource-pack overrides and dye selection before flattening worn item silhouettes.
public final class PufferfishWearModel extends BakedModelWrapper<BakedModel> {
    private final ItemOverrides overrides;

    public PufferfishWearModel(BakedModel original) {
        super(original);
        Map<BakedModel, BakedModel[]> stages = new IdentityHashMap<>();
        overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                var resolved = original.getOverrides().resolve(original, stack, level, entity, seed);
                if (resolved == null) resolved = original;
                int stage = PufferfishDurability.deflationStage(stack);
                if (stage == 0 || resolved.isCustomRenderer()) return resolved;
                var variants = stages.computeIfAbsent(resolved, key -> new BakedModel[4]);
                if (variants[stage] == null) variants[stage] = new Flattened(resolved, stage);
                return variants[stage];
            }
        };
    }

    @Override public ItemOverrides getOverrides() { return overrides; }

    private static final class Flattened extends BakedModelWrapper<BakedModel> {
        private final int stage;
        private final Map<BakedModel, BakedModel> passes = new IdentityHashMap<>();

        private Flattened(BakedModel original, int stage) { super(original); this.stage = stage; }

        private List<BakedQuad> flatten(List<BakedQuad> quads) {
            return quads.stream().map(quad -> {
                var vertices = quad.getVertices().clone();
                int stride = vertices.length / 4;
                for (int i = 0; i < vertices.length; i += stride) {
                    float x = Float.intBitsToFloat(vertices[i]);
                    float y = Float.intBitsToFloat(vertices[i + 1]);
                    vertices[i] = Float.floatToRawIntBits(0.5F + (x - 0.5F) * DeflatingPufferfishModel.width(stage));
                    vertices[i + 1] = Float.floatToRawIntBits(0.5F + (y - 0.5F) * Math.max(0.45F, DeflatingPufferfishModel.height(stage)));
                }
                return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
            }).toList();
        }

        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return flatten(originalModel.getQuads(state, side, random));
        }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
            return flatten(originalModel.getQuads(state, side, random, data, type));
        }
        @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean left) {
            var transformed = originalModel.applyTransform(context, pose, left);
            return transformed == originalModel ? this : new Flattened(transformed, stage);
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return originalModel.getRenderPasses(stack, fabulous).stream().map(pass -> pass == originalModel ? this
                    : passes.computeIfAbsent(pass, key -> new Flattened(key, stage))).toList();
        }
    }
}
