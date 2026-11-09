// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.glavo.kaleidoscope.pufferfish.PufferfishBoats;

public final class PufferfishBoat extends Boat {
    private final String skinBoatName;

    public PufferfishBoat(EntityType<? extends Boat> type, Level level, String skinBoatName) {
        super(type, level);
        this.skinBoatName = skinBoatName;
    }

    public String skinBoatName() { return skinBoatName; }

    @Override
    public Item getDropItem() { return PufferfishBoats.VARIANTS.get(skinBoatName).item().get(); }

    /// Falls return the skin boat rather than vanilla wooden planks.
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        if (distance > 3 && !level().isClientSide && !isRemoved()) {
            destroy(source);
            return true;
        }
        return false;
    }
}
