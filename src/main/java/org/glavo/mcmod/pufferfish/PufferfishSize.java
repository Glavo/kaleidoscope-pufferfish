// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class PufferfishSize {
    private static final net.minecraft.resources.ResourceLocation MODIFIER = PufferfishPlease.id("cultivation_scale");

    private PufferfishSize() {}

    public static float scale(ItemStack stack) {
        var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
        return cultivation == null ? 1 : cultivation.level().scale();
    }

    public static float equipmentScale(ItemStack stack) { return Math.min(scale(stack), 1.25F); }

    /// The native attribute synchronizes geometry and collision size without replacing external modifiers.
    public static void update(Pufferfish fish) {
        if (fish.level().isClientSide) return;
        var stack = PufferfishData.stored(fish);
        var attribute = fish.getAttribute(Attributes.SCALE);
        double amount = scale(stack) - 1;
        var previous = attribute.getModifier(MODIFIER);
        if (previous != null && previous.amount() == amount) return;
        attribute.removeModifier(MODIFIER);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(MODIFIER, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        fish.refreshDimensions();
    }

    public static void joinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Pufferfish fish) update(fish);
    }
}
