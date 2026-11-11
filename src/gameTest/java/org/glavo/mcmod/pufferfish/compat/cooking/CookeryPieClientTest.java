// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.compat.cooking;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import org.glavo.mcmod.pufferfish.compat.cookery.CookeryFoodIntegration;

public final class CookeryPieClientTest {
    public static void verify(Minecraft minecraft) {
        for (var block : java.util.List.of(CookeryFoodIntegration.STARGAZY_PIE.get(), CookeryFoodIntegration.ASPIC.get())) {
            int previous = Integer.MAX_VALUE;
            for (int bites : block.getBites().getPossibleValues().stream().sorted().toList()) {
                var state = block.defaultBlockState().setValue(block.getBites(), bites);
                var model = minecraft.getBlockRenderer().getBlockModel(state);
                int quads = model.getQuads(state, null, RandomSource.create(0)).size();
                if (model == minecraft.getModelManager().getMissingModel() || quads <= 0 || quads >= previous) {
                    throw new IllegalStateException("Missing or non-decreasing meal portion model: " + state);
                }
                previous = quads;
            }
        }
    }
}
