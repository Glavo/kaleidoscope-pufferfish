// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.glavo.kaleidoscope.pufferfish.PufferfishBoats;

public final class PufferfishBoat extends Boat {
    public static final float FRONT_SEAT = 0.2F;
    public static final float REAR_SEAT = -0.75F;
    private final String skinBoatName;

    public PufferfishBoat(EntityType<? extends Boat> type, Level level, String skinBoatName) {
        super(type, level);
        this.skinBoatName = skinBoatName;
    }

    public String skinBoatName() { return skinBoatName; }

    public boolean isHeavy() { return skinBoatName.equals("heavy_pufferfish_skin_boat"); }

    public void sinkInWater() {
        setDeltaMovement(getDeltaMovement().add(0, -getGravity(), 0).multiply(0.9F, 0.9F, 0.9F));
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return isHeavy() ? getPassengers().size() < getMaxPassengers() : super.canAddPassenger(passenger);
    }

    @Override
    public Item getDropItem() { return PufferfishBoats.VARIANTS.get(skinBoatName).item().get(); }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        float seat = getPassengers().indexOf(passenger) > 0 ? REAR_SEAT : FRONT_SEAT;
        return new Vec3(0, 0.33, seat).yRot(-getYRot() * (float) (Math.PI / 180));
    }

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
