// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MaceItem;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MaceItem.class)
public abstract class PufferfishSmashKnockbackMixin {
    @Inject(method = "knockbackPredicate", at = @At("RETURN"), cancellable = true)
    private static void pufferfishPlease$protectCats(Player player, Entity target, CallbackInfoReturnable<Predicate<LivingEntity>> callback) {
        if (PufferfishItems.isHeavy(player.getMainHandItem())) {
            callback.setReturnValue(callback.getReturnValue().and(entity -> !(entity instanceof Cat)));
        }
    }
}
