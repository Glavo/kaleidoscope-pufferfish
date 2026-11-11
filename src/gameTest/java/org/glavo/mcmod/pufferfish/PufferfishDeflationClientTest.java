// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.client.DeflatingPufferfishModel;
import org.glavo.mcmod.pufferfish.test.TestItems;

public final class PufferfishDeflationClientTest {
    public static void verify(Minecraft minecraft) {
        var geometry = new DeflatingPufferfishModel<Entity>(minecraft.getEntityModels());
        for (var original : TestItems.fishVariants()) {
            for (int stage = 0; stage < 4; stage++) {
                var stack = worn(original, stage);
                var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
                if (model.isCustomRenderer() != (stage > 0)) {
                    throw new IllegalStateException("Worn fish did not select puff-state item rendering: " + stack);
                }
                geometry.prepare(stack);
                var root = geometry.root();
                var body = root.getChild("body").getRandomCube(RandomSource.create(0));
                float expectedWidth = stage == 0 ? 8 : stage == 3 ? 3 : 5;
                float expectedHeight = stage == 3 ? 2 : expectedWidth;
                if (body.maxX - body.minX != expectedWidth || body.maxY - body.minY != expectedHeight
                        || body.maxZ - body.minZ != expectedWidth || root.xScale != root.yScale || root.yScale != root.zScale) {
                    throw new IllegalStateException("Puff-state geometry was flattened instead of using vanilla proportions");
                }
                if (stage == 3 && (!root.hasChild("left_eye") || !root.hasChild("right_eye") || !root.hasChild("back_fin"))) {
                    throw new IllegalStateException("Deflated fish lost its raised eyes or tail");
                }
            }
            geometry.prepare(original);
            if (geometry.root().getChild("body").yScale != 1 || geometry.root().getChild("body").y != 22) {
                throw new IllegalStateException("Shared fish model retained deformation from another stack");
            }
        }
        var pump = PufferfishItems.PUMP.toStack();
        if (!minecraft.getItemRenderer().getModel(pump, null, null, 0).isCustomRenderer()
                || pump.getHoverName().getString().startsWith("block.")) {
            throw new IllegalStateException("Pump model or translation is missing");
        }
    }

    private static ItemStack worn(ItemStack original, int stage) {
        var stack = original.copyWithCount(1);
        PufferfishDurability.activate(stack);
        stack.setDamageValue((stack.getMaxDamage() * stage + 3) / 4);
        return stack;
    }

}
