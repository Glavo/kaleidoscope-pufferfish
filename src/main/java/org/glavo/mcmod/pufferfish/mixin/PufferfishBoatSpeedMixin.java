// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.world.entity.vehicle.Boat;
import org.glavo.mcmod.pufferfish.entity.PufferfishBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Boat.class)
public abstract class PufferfishBoatSpeedMixin {
    /// Scales paddle thrust without changing drag, buoyancy, or turning speed.
    @ModifyArgs(method = "controlBoat", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private void pufferfishPlease$kayakThrust(Args args) {
        if ((Object) this instanceof PufferfishBoat boat) {
            double multiplier = boat.skinBoatName().equals("radiant_pufferfish_skin_boat") ? 1.5 : 1.25;
            args.set(0, (double) args.get(0) * multiplier);
            args.set(2, (double) args.get(2) * multiplier);
        }
    }
}
