// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.glavo.kaleidoscope.pufferfish.PufferfishDurability;

/// Selects three-dimensional puff-state rendering for worn fish while retaining pristine item overrides.
public final class PufferfishWearModel extends BakedModelWrapper<BakedModel> {
    private final ItemOverrides overrides;

    public PufferfishWearModel(BakedModel original) {
        super(original);
        Map<BakedModel, BakedModel> models = new IdentityHashMap<>();
        overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                var resolved = original.getOverrides().resolve(original, stack, level, entity, seed);
                if (resolved == null) resolved = original;
                if (PufferfishDurability.deflationStage(stack) == 0 || resolved.isCustomRenderer()) return resolved;
                return models.computeIfAbsent(resolved, Worn::new);
            }
        };
    }

    @Override public ItemOverrides getOverrides() { return overrides; }

    private static final class Worn extends BakedModelWrapper<BakedModel> {
        private Worn(BakedModel original) { super(original); }
        @Override public boolean isCustomRenderer() { return true; }
        @Override public boolean isGui3d() { return true; }
        @Override public boolean usesBlockLight() { return true; }
        @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean left) {
            originalModel.applyTransform(context, pose, left);
            return this;
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
    }
}
