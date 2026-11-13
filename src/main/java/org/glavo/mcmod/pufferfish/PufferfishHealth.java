// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pufferfish;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class PufferfishHealth {
    private static final ResourceLocation MODIFIER = PufferfishPlease.id("cultivation_health");

    private PufferfishHealth() {}

    /// Rank changes preserve the health percentage and leave external modifiers intact.
    public static void update(Pufferfish fish) {
        if (fish.level().isClientSide) return;
        var cultivation = PufferfishData.stored(fish).get(PufferfishPlease.CULTIVATION.get());
        double amount = cultivation == null ? 0 : cultivation.level().healthMultiplier() - 1;
        var attribute = fish.getAttribute(Attributes.MAX_HEALTH);
        var previous = attribute.getModifier(MODIFIER);
        if (previous == null ? amount == 0 : previous.amount() == amount) return;
        float fraction = fish.getHealth() / fish.getMaxHealth();
        attribute.removeModifier(MODIFIER);
        // Persist the modifier so vanilla restores maximum health before loading current health.
        if (amount != 0) attribute.addPermanentModifier(new AttributeModifier(MODIFIER, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        fish.setHealth(fraction * fish.getMaxHealth());
    }

    public static void joinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Pufferfish fish) update(fish);
    }
}
