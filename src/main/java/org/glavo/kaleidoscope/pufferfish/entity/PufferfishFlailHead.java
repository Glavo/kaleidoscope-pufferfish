// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishCombat;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

/// A temporary flail head that returns after impact or reaching its tether length.
/// The weapon stays in its owner's hand; removing this entity never creates an item.
public final class PufferfishFlailHead extends ThrowableItemProjectile {
    /// The maximum outward distance from the owner's eye position, in blocks.
    public static final double REACH = 6;
    /// The maximum lifetime in ticks, shorter than the weapon's launch cooldown.
    private static final int LIFETIME = 24;
    /// Whether the head has finished its damaging outward flight.
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.BOOLEAN);
    /// Whether the weapon was launched from the off hand.
    private static final EntityDataAccessor<Boolean> OFF_HAND = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.BOOLEAN);

    /// Creates a head for entity loading or network spawning.
    public PufferfishFlailHead(EntityType<? extends PufferfishFlailHead> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /// Creates a head holding a copy of the weapon in the owner's specified hand.
    public PufferfishFlailHead(Level level, Player owner, InteractionHand hand) {
        super(KaleidoscopePufferfish.FLAIL_HEAD.get(), owner, level);
        entityData.set(OFF_HAND, hand == InteractionHand.OFF_HAND);
        setItem(owner.getItemInHand(hand));
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RETURNING, false);
        builder.define(OFF_HAND, false);
    }

    /// Returns the hand holding this head's weapon.
    public InteractionHand getHand() {
        return entityData.get(OFF_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /// Returns whether the head is returning without dealing further damage.
    public boolean isReturning() {
        return entityData.get(RETURNING);
    }

    @Override
    protected Item getDefaultItem() { return PufferfishItems.FLAIL.get(); }

    @Override
    public ItemStack getWeaponItem() { return getItem(); }

    /// Prevents the tethered head from entering another dimension through a portal.
    @Override
    public boolean canUsePortal(boolean allowPassengers) { return false; }

    /// Advances the head, discarding it when its owner or held weapon is no longer valid.
    /// Returning heads pass through blocks and entities and disappear at the owner's hand.
    @Override
    public void tick() {
        var owner = getOwner();
        if (!level().isClientSide() && (!(owner instanceof Player player) || !player.isAlive()
                || player.isSpectator() || player.level() != level()
                || !ItemStack.isSameItemSameComponents(player.getItemInHand(getHand()), getItem())
                || tickCount >= LIFETIME)) {
            discard();
            return;
        }
        if (owner == null) {
            super.tick();
            return;
        }
        Vec3 target = owner.getEyePosition().add(0, -0.3, 0);
        if (!isReturning() && (tickCount >= 8 || position().add(getDeltaMovement()).distanceTo(owner.getEyePosition()) >= REACH)) {
            beginReturn();
        }
        if (isReturning()) {
            baseTick();
            Vec3 offset = target.subtract(position());
            if (offset.lengthSqr() <= 0.64) {
                if (!level().isClientSide()) discard();
                return;
            }
            setDeltaMovement(offset.normalize().scale(Math.min(1.4, offset.length())));
            setPos(position().add(getDeltaMovement()));
            updateRotation();
        } else {
            super.tick();
        }
    }

    /// Switches to a collision-free return flight.
    private void beginReturn() {
        entityData.set(RETURNING, true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !isReturning() && target != getOwner() && super.canHitEntity(target);
    }

    /// Applies the weapon's damage and poison once, on a successful outward impact.
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide() || isReturning()) return;
        var target = result.getEntity();
        var source = new DamageSource(registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ThrownPufferfish.DAMAGE_TYPE), this, getOwner());
        if (target.hurt(source, PufferfishCombat.damage(getItem())) && target instanceof LivingEntity living) {
            PufferfishCombat.hitEffects(getItem(), living, getEffectSource(), getDeltaMovement());
        }
    }

    /// Starts returning after the first impact, including an impact that deals no damage.
    @Override
    protected void onHit(HitResult result) {
        if (isReturning() || isRemoved()) return;
        super.onHit(result);
        beginReturn();
    }
}
