// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;

public class ThrownPufferfish extends ThrowableItemProjectile {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, KaleidoscopePufferfish.id("thrown_pufferfish"));

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
            var damage = new DamageSource(registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE),
                    this, getOwner()) {
                @Override
                public Component getLocalizedDeathMessage(LivingEntity victim) {
                    String key = "death.attack." + getMsgId();
                    var attacker = getEntity();
                    return attacker == null
                            ? Component.translatable(key, victim.getDisplayName())
                            : Component.translatable(key + ".player", victim.getDisplayName(), attacker.getDisplayName());
                }
            };
            if (target.hurt(damage, 2.0F) && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0), getEffectSource());
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (isRemoved()) {
            return;
        }
        super.onHit(result);
        if (!level().isClientSide()) {
            Vec3 position = result.getLocation();
            if (result instanceof BlockHitResult blockHit) {
                // Keep the recovered item outside the block's collision surface.
                position = position.add(Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()).scale(0.25));
            }
            var recovered = new ItemEntity(level(), position.x, position.y, position.z, getItem().copyWithCount(1));
            recovered.setDeltaMovement(0.0, 0.1, 0.0);
            recovered.setDefaultPickUpDelay();
            level().addFreshEntity(recovered);
            discard();
        }
    }
}
