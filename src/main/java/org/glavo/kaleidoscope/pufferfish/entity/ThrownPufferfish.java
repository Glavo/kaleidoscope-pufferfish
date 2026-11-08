// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;

public class ThrownPufferfish extends ThrowableItemProjectile {
    public ThrownPufferfish(EntityType<? extends ThrownPufferfish> type, Level level) {
        super(type, level);
    }

    public ThrownPufferfish(Level level, LivingEntity owner) {
        super(KaleidoscopePufferfish.THROWN_PUFFERFISH.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.PUFFERFISH;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide()) {
            var target = result.getEntity();
            if (target.hurt(damageSources().thrown(this, getOwner()), 2.0F) && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0), getEffectSource());
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == 3) {
            var particle = new ItemParticleOption(ParticleTypes.ITEM, getItem());
            for (int i = 0; i < 8; i++) {
                level().addParticle(particle, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            }
        } else {
            super.handleEntityEvent(event);
        }
    }
}
