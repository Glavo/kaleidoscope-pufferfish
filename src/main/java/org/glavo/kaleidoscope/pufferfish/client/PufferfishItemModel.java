// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;

public final class PufferfishItemModel extends BakedModelWrapper<BakedModel> {
    private final ItemOverrides overrides;

    public PufferfishItemModel(BakedModel original, Map<DyeColor, BakedModel> coloredModels) {
        super(original);
        Map<DyeColor, BakedModel> models = Map.copyOf(coloredModels);
        overrides = new ItemOverrides() {
            @Override
            @Nullable
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                      @Nullable LivingEntity entity, int seed) {
                BakedModel resolved = original.getOverrides().resolve(original, stack, level, entity, seed);
                // Explicit overrides from resource packs or other mods take precedence.
                if (resolved != original) {
                    return resolved;
                }
                DyeColor color = PufferfishColors.get(stack);
                return color == null ? original : models.getOrDefault(color, original);
            }
        };
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
