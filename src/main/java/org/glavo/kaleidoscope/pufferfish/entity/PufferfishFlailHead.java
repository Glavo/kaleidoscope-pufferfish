// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import org.glavo.kaleidoscope.pufferfish.PufferfishFlailItem;
import org.glavo.kaleidoscope.pufferfish.PufferfishFlailMotion;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishCombat;
import org.glavo.kaleidoscope.pufferfish.PufferfishChanneling;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

/// A temporary flail head that returns after impact or reaching its tether length.
/// The weapon stays in its owner's hand; removing this entity never creates an item.
public final class PufferfishFlailHead extends ThrowableItemProjectile {
    /// The maximum outward distance from the owner's eye position, in blocks.
    public static final double REACH = 6;
    public static final double SHORT_REACH = 2.5;
    /// The maximum return speed, in blocks per tick.
    private static final double RETURN_SPEED = 0.4;
    /// The distance from the return target at which the head is removed, in blocks.
    private static final double RETURN_DISTANCE = 0.2;
    /// The maximum flight lifetime in ticks, excluding charging and queued release.
    private static final int LIFETIME = 29;
    /// Whether the head has finished its damaging outward flight.
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.BOOLEAN);
    /// Whether the weapon was launched from the off hand.
    private static final EntityDataAccessor<Boolean> OFF_HAND = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHORT_CAST = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> QUEUED_AGE = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> PHASE_TIME = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> RELEASE_AGE = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> FLIGHT_TIME = SynchedEntityData.defineId(PufferfishFlailHead.class, EntityDataSerializers.LONG);
    private float charge = 1;
    private int phaseTick;
    private int flightStartTick;

    /// Creates a head for entity loading or network spawning.
    public PufferfishFlailHead(EntityType<? extends PufferfishFlailHead> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /// Creates a head holding a copy of the weapon in the owner's specified hand.
    public PufferfishFlailHead(Level level, Player owner, InteractionHand hand) {
        this(level, owner, hand, 1);
    }

    public PufferfishFlailHead(Level level, Player owner, InteractionHand hand, float charge) {
        super(KaleidoscopePufferfish.FLAIL_HEAD.get(), owner, level);
        this.charge = Float.isFinite(charge) ? Mth.clamp(charge, 0, 1) : 0;
        entityData.set(OFF_HAND, hand == InteractionHand.OFF_HAND);
        setItem(owner.getItemInHand(hand));
        setNoGravity(true);
    }

    public boolean isCharging() { return entityData.get(PHASE) == 1; }
    public boolean isQueued() { return entityData.get(PHASE) == 2; }
    public boolean isSpinning() { return isCharging() || isQueued(); }

    public boolean isShortCast() { return entityData.get(SHORT_CAST); }

    private double reach() { return isShortCast() ? SHORT_REACH : REACH; }

    /// Starts a close-range cast from the holding hand without entering the charging orbit.
    public void beginShortCast() {
        if (!(getOwner() instanceof Player player)) return;
        entityData.set(SHORT_CAST, true);
        setPos(PufferfishFlailMotion.grip(player, getHand(), player.getYRot(), 1));
        setOldPosAndRot();
        setDeltaMovement(aimPoint(player, player.getLookAngle()).subtract(position()).normalize().scale(0.8));
        flightStartTick = tickCount;
        hasImpulse = true;
    }

    public void beginCharging() {
        entityData.set(PHASE, 1);
        entityData.set(PHASE_TIME, level().getGameTime());
        if (getOwner() instanceof Player player) {
            setPos(PufferfishFlailMotion.orbit(player, getHand(), getItem(), 0, player.getYRot(), 1));
            setOldPosAndRot();
        }
    }

    public void queueRelease(float age, float yaw, float pitch) {
        if (!isCharging()) return;
        charge = PufferfishFlailItem.charge(getItem(), age);
        entityData.set(QUEUED_AGE, age);
        entityData.set(RELEASE_AGE, PufferfishFlailMotion.releaseAge(getItem(), age, pitch));
        entityData.set(AIM_YAW, yaw);
        entityData.set(PHASE_TIME, level().getGameTime());
        phaseTick = tickCount;
        entityData.set(PHASE, 2);
    }

    public float orbitAge(float partialTick) {
        if (isCharging()) {
            if (getOwner() instanceof Player player && player.isUsingItem()) return player.getTicksUsingItem() + partialTick;
            return Math.max(0, level().getGameTime() - entityData.get(PHASE_TIME)) + partialTick;
        }
        float elapsed = level().isClientSide()
                ? Math.max(0, level().getGameTime() - entityData.get(PHASE_TIME)) : tickCount - phaseTick;
        return Math.min(entityData.get(RELEASE_AGE), entityData.get(QUEUED_AGE) + elapsed + partialTick);
    }

    public Vec3 renderPosition(float partialTick) {
        if (isSpinning() && getOwner() instanceof Player player) {
            return PufferfishFlailMotion.orbit(player, getHand(), getItem(), orbitAge(partialTick),
                    isQueued() ? entityData.get(AIM_YAW) : player.getViewYRot(partialTick), partialTick);
        }
        return getPosition(partialTick);
    }

    public boolean hasSwing() { return !isShortCast() && (isSpinning() || entityData.get(FLIGHT_TIME) >= 0); }

    public float swingYaw(float partialTick) {
        return isCharging() && getOwner() instanceof Player player ? player.getViewYRot(partialTick) : entityData.get(AIM_YAW);
    }

    public float armAngle(float partialTick) {
        if (isSpinning()) return (float) PufferfishFlailItem.spinAngle(getItem(), orbitAge(partialTick));
        double release = PufferfishFlailItem.spinAngle(getItem(), entityData.get(RELEASE_AGE));
        double turn = Math.floor(release / (Math.PI * 2)) * Math.PI * 2;
        double forward = Math.max(release, turn + Math.PI * 1.5);
        float elapsed = (level().isClientSide() ? level().getGameTime() - entityData.get(FLIGHT_TIME)
                : tickCount - flightStartTick) + partialTick;
        float follow = Mth.clamp(elapsed / 6, 0, 1);
        float recover = Mth.clamp((elapsed - 8) / 10, 0, 1);
        return (float) (release + (forward - release) * follow * follow * (3 - 2 * follow)
                + (turn + Math.PI * 2 - forward) * recover * recover * (3 - 2 * recover));
    }

    private void launch(Player player) {
        float age = entityData.get(RELEASE_AGE);
        float yaw = entityData.get(AIM_YAW);
        setPos(PufferfishFlailMotion.orbit(player, getHand(), getItem(), age, yaw, 1));
        var direction = PufferfishFlailMotion.tangent(getItem(), age, yaw);
        var aim = aimPoint(player, direction);
        setDeltaMovement(aim.subtract(position()).normalize().scale(1.2));
        hasImpulse = true;
        entityData.set(FLIGHT_TIME, level().getGameTime());
        entityData.set(PHASE, 0);
        flightStartTick = tickCount;
        if (!level().isClientSide()) {
            player.getCooldowns().addCooldown(PufferfishItems.FLAIL.get(), PufferfishFlailItem.COOLDOWN);
            player.getCooldowns().addCooldown(PufferfishItems.NETHERITE_FLAIL.get(), PufferfishFlailItem.COOLDOWN);
            player.getCooldowns().addCooldown(PufferfishItems.HEAVY_FLAIL.get(), PufferfishFlailItem.COOLDOWN);
            player.awardStat(Stats.ITEM_USED.get(getItem().getItem()));
            level().playSound(null, position().x, position().y, position().z, SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8F, 1);
        }
    }

    /// Converges on the sight-line hit while preserving the actual orbital release position.
    private Vec3 aimPoint(Player player, Vec3 direction) {
        var eye = player.getEyePosition();
        var end = eye.add(direction.scale(reach()));
        var block = level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        var entity = ProjectileUtil.getEntityHitResult(player, eye, end, new AABB(eye, end).inflate(1),
                candidate -> candidate.isAlive() && candidate.isPickable() && !candidate.isSpectator()
                        && !candidate.isPassengerOfSameVehicle(player), eye.distanceToSqr(end));
        return entity != null ? entity.getLocation() : end;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Charge", charge);
        tag.putBoolean("OffHand", getHand() == InteractionHand.OFF_HAND);
        tag.putBoolean("Returning", isReturning());
        tag.putBoolean("ShortCast", isShortCast());
        tag.putInt("Phase", entityData.get(PHASE));
        tag.putFloat("OrbitAge", orbitAge(0));
        tag.putFloat("ReleaseAge", entityData.get(RELEASE_AGE));
        tag.putFloat("AimYaw", entityData.get(AIM_YAW));
        tag.putInt("FlightAge", tickCount - flightStartTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        float saved = tag.contains("Charge") ? tag.getFloat("Charge") : 1;
        charge = Float.isFinite(saved) ? Mth.clamp(saved, 0, 1) : 0;
        entityData.set(OFF_HAND, tag.getBoolean("OffHand"));
        entityData.set(RETURNING, tag.getBoolean("Returning"));
        entityData.set(SHORT_CAST, tag.getBoolean("ShortCast"));
        entityData.set(PHASE, Mth.clamp(tag.getInt("Phase"), 0, 2));
        entityData.set(QUEUED_AGE, tag.getFloat("OrbitAge"));
        entityData.set(RELEASE_AGE, tag.getFloat("ReleaseAge"));
        entityData.set(AIM_YAW, tag.getFloat("AimYaw"));
        entityData.set(PHASE_TIME, level().getGameTime());
        phaseTick = tickCount;
        flightStartTick = tickCount - tag.getInt("FlightAge");
        entityData.set(FLIGHT_TIME, level().getGameTime() - tag.getInt("FlightAge"));
        noPhysics = isReturning();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RETURNING, false);
        builder.define(OFF_HAND, false);
        builder.define(SHORT_CAST, false);
        builder.define(PHASE, 0);
        builder.define(QUEUED_AGE, 0F);
        builder.define(PHASE_TIME, 0L);
        builder.define(RELEASE_AGE, 0F);
        builder.define(AIM_YAW, 0F);
        builder.define(FLIGHT_TIME, -1L);
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
                || (!isSpinning() && tickCount - flightStartTick >= LIFETIME))) {
            discard();
            return;
        }
        if (owner == null) {
            super.tick();
            return;
        }
        if (isSpinning() && owner instanceof Player player) {
            baseTick();
            if (isCharging() && !level().isClientSide() && (!player.isUsingItem()
                    || player.getUsedItemHand() != getHand())) {
                discard();
                return;
            }
            if (isQueued() && orbitAge(0) >= entityData.get(RELEASE_AGE)) launch(player);
            else setPos(renderPosition(1));
            return;
        }
        Vec3 target = owner instanceof Player player
                ? PufferfishFlailMotion.grip(player, getHand(), player.getYRot(), 1) : owner.getEyePosition();
        long flightAge = level().isClientSide() && entityData.get(FLIGHT_TIME) >= 0
                ? Math.max(0, level().getGameTime() - entityData.get(FLIGHT_TIME)) : tickCount - flightStartTick;
        if (!isReturning() && (flightAge >= 8 || position().distanceToSqr(owner.getEyePosition()) >= reach() * reach())) {
            beginReturn();
        }
        if (isReturning()) {
            baseTick();
            Vec3 offset = target.subtract(position());
            if (offset.lengthSqr() <= RETURN_DISTANCE * RETURN_DISTANCE) {
                if (!level().isClientSide()) discard();
                return;
            }
            setDeltaMovement(offset.normalize().scale(Math.min(RETURN_SPEED, offset.length())));
            setPos(position().add(getDeltaMovement()));
            updateRotation();
        } else {
            if (isShortCast()) {
                var overlapping = level().getEntities(this, getBoundingBox().inflate(0.3), this::canHitEntity).stream()
                        .filter(entity -> entity.getBoundingBox().inflate(0.3).contains(position()))
                        .min(java.util.Comparator.comparingDouble(this::distanceToSqr));
                if (overlapping.isPresent()) {
                    onHit(new EntityHitResult(overlapping.get(), position()));
                    return;
                }
            }
            boolean atLimit = clipToReach(owner.getEyePosition());
            super.tick();
            if (atLimit && !isReturning() && !isRemoved()) beginReturn();
        }
    }

    /// Keeps the last outward segment inside the tether sphere without skipping its collision test.
    private boolean clipToReach(Vec3 eye) {
        var movement = getDeltaMovement();
        if (position().add(movement).distanceToSqr(eye) <= reach() * reach()) return false;
        var relative = position().subtract(eye);
        double a = movement.lengthSqr();
        if (a < 1.0e-12) return false;
        double b = relative.dot(movement);
        double c = relative.lengthSqr() - reach() * reach();
        double fraction = Mth.clamp((-b + Math.sqrt(Math.max(0, b * b - a * c))) / a, 0, 1);
        setDeltaMovement(movement.scale(fraction));
        return true;
    }

    /// Switches to a collision-free return flight.
    private void beginReturn() {
        entityData.set(RETURNING, true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !isSpinning() && !isReturning() && target != getOwner() && super.canHitEntity(target);
    }

    /// Applies the weapon's damage and poison once, on a successful outward impact.
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel server) || isReturning() || isSpinning()) return;
        var target = result.getEntity();
        var source = new DamageSource(registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ThrownPufferfish.DAMAGE_TYPE), this, getOwner());
        float fullChargeMultiplier = PufferfishItems.hasHeavyCombat(getItem()) ? 1.6F : 2;
        float multiplier = isShortCast() ? 1 : fullChargeMultiplier * charge;
        if (target.hurt(source, PufferfishCombat.projectileDamage(server, getItem(), target, source) * multiplier)) {
            if (target instanceof LivingEntity living) {
                PufferfishCombat.hitEffects(getItem(), living, getEffectSource(), getDeltaMovement());
            }
            PufferfishChanneling.hitEntity(this, getItem(), target);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        PufferfishChanneling.hitBlock(this, getItem(), result);
    }

    /// Starts returning after the first impact, including an impact that deals no damage.
    @Override
    protected void onHit(HitResult result) {
        if (isSpinning() || isReturning() || isRemoved()) return;
        Vec3 impact = result.getLocation();
        if (result instanceof EntityHitResult entityHit) {
            var bounds = entityHit.getEntity().getBoundingBox().inflate(0.3);
            impact = bounds.contains(position()) ? position()
                    : bounds.clip(position(), position().add(getDeltaMovement())).orElse(position());
        }
        setPos(impact);
        super.onHit(result);
        beginReturn();
    }
}
