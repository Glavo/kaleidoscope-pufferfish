// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import net.minecraft.world.entity.vehicle.Boat;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public abstract class PufferfishBoatBuoyancyMixin {
    @Shadow private Boat.Status status;
    @Shadow private float deltaRotation;
    @Shadow private float invFriction;
    @Shadow private float outOfControlTicks;

    @Inject(method = "floatBoat", at = @At("HEAD"), cancellable = true)
    private void kaleidoscope$sinkHeavyKayak(CallbackInfo callback) {
        if ((Object) this instanceof PufferfishBoat boat && boat.isHeavy()
                && (status == Boat.Status.IN_WATER || status == Boat.Status.UNDER_WATER || status == Boat.Status.UNDER_FLOWING_WATER)) {
            invFriction = 0.9F;
            deltaRotation *= invFriction;
            boat.sinkInWater();
            callback.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void kaleidoscope$keepHeavyKayakPassengers(CallbackInfo callback) {
        if ((Object) this instanceof PufferfishBoat boat && boat.isHeavy()) outOfControlTicks = 0;
    }
}
