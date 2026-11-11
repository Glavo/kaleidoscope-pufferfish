// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pufferfish;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pufferfish.class)
public abstract class MaterialPufferfishMixin {
    @Shadow int inflateCounter;
    @Shadow int deflateTimer;

    /// Combat controls inflation while winding up and recovering, without repeated inflation sounds.
    @Inject(method = "tick", at = @At("HEAD"))
    private void pufferfishPlease$combatInflation(CallbackInfo callback) {
        if ((Object) this instanceof MaterialPufferfish fish && fish.attackPhase() != MaterialPufferfish.IDLE) {
            inflateCounter = 0;
            deflateTimer = 0;
        }
    }

    @Inject(method = "touch", at = @At("HEAD"), cancellable = true)
    private void pufferfishPlease$sting(Mob target, CallbackInfo callback) {
        if ((Object) this instanceof MaterialPufferfish fish) {
            fish.sting(target);
            callback.cancel();
        }
    }
}
