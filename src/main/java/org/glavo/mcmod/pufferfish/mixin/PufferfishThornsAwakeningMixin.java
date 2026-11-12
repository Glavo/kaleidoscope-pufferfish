// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.DamageEntity;
import net.minecraft.world.phys.Vec3;
import org.glavo.mcmod.pufferfish.PufferfishAwakening;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DamageEntity.class)
public abstract class PufferfishThornsAwakeningMixin {
    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean pufferfishPlease$creditThornsItem(Entity target, DamageSource source, float amount, Operation<Boolean> original,
            ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        if (source.is(DamageTypes.THORNS) && target instanceof LivingEntity living) {
            return PufferfishAwakening.track(item.itemStack(), item.owner(), living, source, () -> original.call(target, source, amount));
        }
        return original.call(target, source, amount);
    }
}
