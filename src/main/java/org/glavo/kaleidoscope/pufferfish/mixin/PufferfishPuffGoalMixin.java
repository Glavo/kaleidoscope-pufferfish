// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import org.glavo.kaleidoscope.pufferfish.PufferfishHeadgear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/// Disguise affects proximity inflation only, never contact damage from an already inflated fish.
@Mixin(targets = "net.minecraft.world.entity.animal.Pufferfish$PufferfishPuffGoal")
public abstract class PufferfishPuffGoalMixin {
    @ModifyArg(method = "canUse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"), index = 2)
    private Predicate<LivingEntity> kaleidoscope$ignoreDisguisedPlayers(Predicate<LivingEntity> predicate) {
        return entity -> !PufferfishHeadgear.isWearing(entity) && predicate.test(entity);
    }
}
