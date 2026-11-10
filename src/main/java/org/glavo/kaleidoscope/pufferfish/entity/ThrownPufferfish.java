// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishCombat;
import org.glavo.kaleidoscope.pufferfish.PufferfishChanneling;
import org.glavo.kaleidoscope.pufferfish.PufferfishWindBurst;

public class ThrownPufferfish extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(ThrownPufferfish.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RETURN_ACCELERATION = SynchedEntityData.defineId(ThrownPufferfish.class, EntityDataSerializers.INT);
    private boolean creativeThrow;
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, KaleidoscopePufferfish.id("thrown_pufferfish"));

    public ThrownPufferfish(EntityType<? extends ThrownPufferfish> type, Level level) {
        super(type, level);
    }

    public ThrownPufferfish(Level level, LivingEntity owner) {
        super(KaleidoscopePufferfish.THROWN_PUFFERFISH.get(), owner, level);
        creativeThrow = owner instanceof Player player && player.getAbilities().instabuild;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RETURNING, false);
        builder.define(RETURN_ACCELERATION, 0);
    }

    @Override
    public void setItem(ItemStack stack) {
        super.setItem(stack);
        if (level() instanceof ServerLevel server) {
            entityData.set(RETURN_ACCELERATION, Mth.clamp(EnchantmentHelper.getTridentReturnToOwnerAcceleration(server, getItem(), this), 0, 127));
        }
    }

    public boolean isReturning() {
        return entityData.get(RETURNING);
    }

    private boolean canReturnToOwner() {
        return getOwner() instanceof Player player && player.isAlive() && !player.isRemoved()
                && !player.isSpectator() && player.level() == level();
    }

    private void beginReturn() {
        entityData.set(RETURNING, true);
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        hasImpulse = true;
        playSound(SoundEvents.TRIDENT_RETURN, 0.8F, 1.2F);
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && !isReturning() && entityData.get(RETURN_ACCELERATION) > 0
                && getY() < level().getMinBuildHeight() && canReturnToOwner()) {
            beginReturn();
        }
        if (!isReturning()) {
            super.tick();
            return;
        }
        baseTick();
        noPhysics = true;
        setNoGravity(true);
        if (!canReturnToOwner()) {
            if (!level().isClientSide()) {
                recover(position());
            } else {
                setPos(position().add(getDeltaMovement()));
            }
            return;
        }
        var owner = (Player) getOwner();
        Vec3 offset = owner.getEyePosition().add(0, -0.3, 0).subtract(position());
        if (!level().isClientSide() && offset.lengthSqr() <= 1.0) {
            if (creativeThrow) {
                discard();
            } else if (owner.getInventory().add(getItem().copyWithCount(1))) {
                level().playSound(null, owner.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
                discard();
            } else {
                recover(owner.position());
            }
            return;
        }
        Vec3 velocity = getDeltaMovement().scale(0.95)
                .add(offset.normalize().scale(0.05 * entityData.get(RETURN_ACCELERATION)));
        if (velocity.lengthSqr() > offset.lengthSqr()) {
            velocity = offset;
        }
        setDeltaMovement(velocity);
        setPos(position().add(velocity));
        updateRotation();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Returning", isReturning());
        tag.putBoolean("CreativeThrow", creativeThrow);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(RETURNING, tag.getBoolean("Returning"));
        creativeThrow = tag.getBoolean("CreativeThrow");
        noPhysics = isReturning();
        setNoGravity(isReturning());
    }

    @Override
    protected Item getDefaultItem() {
        return Items.PUFFERFISH;
    }

    @Override
    public ItemStack getWeaponItem() {
        return getItem();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel server) {
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
            if (target.hurt(damage, PufferfishCombat.projectileDamage(server, getItem(), target, damage))) {
                if (target instanceof LivingEntity living) {
                    PufferfishCombat.hitEffects(getItem(), living, getEffectSource(), getDeltaMovement());
                }
                PufferfishChanneling.hitEntity(this, getItem(), target);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        PufferfishChanneling.hitBlock(this, getItem(), result);
    }

    @Override
    protected void onHit(HitResult result) {
        if (isRemoved() || isReturning()) {
            return;
        }
        super.onHit(result);
        if (!level().isClientSide()) {
            Vec3 position = result.getLocation();
            if (result instanceof BlockHitResult blockHit) {
                // Keep the recovered item outside the block's collision surface.
                position = position.add(Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()).scale(0.25));
            }
            PufferfishWindBurst.burst(this, position);
            if (!creativeThrow && level() instanceof ServerLevel server) {
                var worn = getItem().copyWithCount(1);
                org.glavo.kaleidoscope.pufferfish.PufferfishDurability.damage(worn, server,
                        getOwner() instanceof LivingEntity living ? living : null, null);
                if (worn.isEmpty()) { discard(); return; }
                setItem(worn);
            }
            if (entityData.get(RETURN_ACCELERATION) > 0 && canReturnToOwner()) {
                setPos(position);
                beginReturn();
            } else {
                recover(position);
            }
        }
    }

    private void recover(Vec3 position) {
        if (!creativeThrow || entityData.get(RETURN_ACCELERATION) == 0) {
            var recovered = new ItemEntity(level(), position.x, position.y, position.z, getItem().copyWithCount(1));
            recovered.setDeltaMovement(0.0, 0.1, 0.0);
            recovered.setDefaultPickUpDelay();
            level().addFreshEntity(recovered);
        }
        discard();
    }
}
