// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.PufferfishCombat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// Adds falling smash hooks to heavy pufferfish.
@Mixin(Item.class)
public abstract class PufferfishSmashMixin {
    /// Adds the configured falling bonus before the normal critical-hit and damage pipeline.
    @Inject(method = "getAttackDamageBonus", at = @At("HEAD"), cancellable = true)
    private void pufferfishPlease$smashBonus(Entity target, float damage, DamageSource source, CallbackInfoReturnable<Float> callback) {
        if (source.getDirectEntity() instanceof LivingEntity attacker && attacker.getWeaponItem().getItem() == (Object) this
                && PufferfishCombat.canSmash(attacker, attacker.getWeaponItem())) {
            callback.setReturnValue(PufferfishCombat.smashBonus(attacker.getWeaponItem(), attacker.fallDistance));
        }
    }

    /// Applies heavy-fish impact feedback and landing protection without consuming the fish.
    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void pufferfishPlease$smashHit(ItemStack stack, LivingEntity target, LivingEntity attacker,
            CallbackInfoReturnable<Boolean> callback) {
        if (PufferfishCombat.canSmash(attacker, stack)) {
            PufferfishCombat.smashHit(attacker, target);
            callback.setReturnValue(true);
        }
    }

    /// Resets the fall distance after successful smash-hit enchantment effects have run.
    @Inject(method = "postHurtEnemy", at = @At("HEAD"))
    private void pufferfishPlease$finishSmash(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfo callback) {
        if (PufferfishCombat.canSmash(attacker, stack)) attacker.resetFallDistance();
    }
}
