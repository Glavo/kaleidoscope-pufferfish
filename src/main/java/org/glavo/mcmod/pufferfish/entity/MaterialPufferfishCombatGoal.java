// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.entity;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.glavo.mcmod.pufferfish.PufferfishMobConfig.Kind;

/// Water-only pursuit with committed rush directions, visible windups and recovery windows.
public final class MaterialPufferfishCombatGoal extends Goal {
    private final MaterialPufferfish fish;
    private int remaining;
    private int decisionDelay;
    private int pulseCooldown = 160;
    private int comboRemaining;
    private Vec3 rushDirection = Vec3.ZERO;

    public MaterialPufferfishCombatGoal(MaterialPufferfish fish) {
        this.fish = fish;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!fish.hasActiveAttacks() || !fish.isInWater()) return false;
        var target = fish.findCombatTarget();
        if (target == null) return false;
        fish.setTarget(target);
        return true;
    }

    @Override
    public boolean canContinueToUse() { return fish.isInWater() && fish.validTarget(fish.getTarget()); }

    @Override
    public void start() {
        decisionDelay = 10;
        fish.setAttackPhase(MaterialPufferfish.IDLE);
    }

    @Override
    public void stop() {
        fish.getNavigation().stop();
        fish.setTarget(null);
        fish.setAttackPhase(MaterialPufferfish.IDLE);
        fish.setDeltaMovement(fish.getDeltaMovement().scale(0.2));
        comboRemaining = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        var target = fish.getTarget();
        if (target == null) return;
        if (pulseCooldown > 0) pulseCooldown--;
        if (decisionDelay > 0) decisionDelay--;
        int phase = fish.attackPhase();
        if (phase != MaterialPufferfish.RUSH) fish.getLookControl().setLookAt(target, 30, 30);
        switch (phase) {
            case MaterialPufferfish.IDLE -> {
                fish.getNavigation().moveTo(target, 1);
                if (!fish.readyToAttack() || decisionDelay > 0) return;
                if (fish.kind() == Kind.RADIANT && pulseCooldown == 0 && fish.distanceToSqr(target) <= 9) {
                    fish.setAttackPhase(MaterialPufferfish.PULSE);
                    remaining = 20;
                    fish.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1, 1.5F);
                } else if (fish.distanceToSqr(target) <= (fish.kind() == Kind.IRON || fish.kind() == Kind.COPPER ? 6.25 : 25)) {
                    comboRemaining = fish.kind() == Kind.NETHERITE && fish.getRandom().nextInt(3) == 0 ? 1 : 0;
                    windup(switch (fish.kind()) { case COPPER, IRON -> 8; case HEAVY -> 20; case NETHERITE -> 10; default -> 12; });
                }
            }
            case MaterialPufferfish.WINDUP -> {
                holdPosition();
                fish.setPuffState(2);
                if (--remaining <= 0) {
                    rushDirection = target.getBoundingBox().getCenter().subtract(fish.getBoundingBox().getCenter()).normalize();
                    fish.setAttackPhase(MaterialPufferfish.RUSH);
                    remaining = fish.kind() == Kind.IRON || fish.kind() == Kind.COPPER ? 6 : 10;
                    fish.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, fish.kind() == Kind.HEAVY ? 0.6F : 1.2F);
                }
            }
            case MaterialPufferfish.RUSH -> rush();
            case MaterialPufferfish.COMBO_GAP -> {
                holdPosition();
                if (--remaining <= 0) windup(6);
            }
            case MaterialPufferfish.RECOVERY, MaterialPufferfish.REST -> {
                holdPosition();
                fish.setPuffState(phase == MaterialPufferfish.RECOVERY ? 0 : 1);
                if (--remaining <= 0) {
                    fish.setAttackPhase(MaterialPufferfish.IDLE);
                    decisionDelay = 8;
                }
            }
            case MaterialPufferfish.PULSE -> {
                holdPosition();
                fish.setPuffState(2);
                if (remaining % 4 == 0 && fish.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.END_ROD, fish.getX(), fish.getY(0.5), fish.getZ(), 8, 0.5, 0.5, 0.5, 0.01);
                }
                if (--remaining <= 0) {
                    fish.pulse();
                    pulseCooldown = 160;
                    recover(false);
                }
            }
        }
    }

    private void holdPosition() {
        fish.getNavigation().stop();
        fish.setDeltaMovement(fish.getDeltaMovement().scale(0.2));
    }

    private void windup(int ticks) {
        fish.setAttackPhase(MaterialPufferfish.WINDUP);
        remaining = ticks;
        fish.setPuffState(2);
        holdPosition();
        fish.playSound(SoundEvents.PUFFER_FISH_BLOW_UP, 1, fish.kind() == Kind.HEAVY ? 0.7F : 1);
    }

    private void rush() {
        double speed = switch (fish.kind()) { case COPPER, IRON -> 0.45; case DIAMOND -> 0.85; case HEAVY -> 0.65; default -> 0.7; };
        var motion = rushDirection.scale(speed);
        var start = fish.getBoundingBox().getCenter();
        var end = start.add(motion);
        var wall = fish.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, fish));
        double wallDistance = wall.getType() == HitResult.Type.MISS ? Double.POSITIVE_INFINITY : start.distanceToSqr(wall.getLocation());
        for (var target : fish.level().getEntitiesOfClass(LivingEntity.class, fish.getBoundingBox().expandTowards(motion).inflate(0.15), fish::canFight)) {
            var bounds = target.getBoundingBox().inflate(fish.getBbWidth() / 2 + 0.1);
            var contact = bounds.contains(start) ? java.util.Optional.of(start) : bounds.clip(start, end);
            if (contact.isPresent() && start.distanceToSqr(contact.get()) <= wallDistance
                    && fish.strike(target, fish.kind() == Kind.IRON || fish.kind() == Kind.COPPER ? 0.3 : fish.kind() == Kind.HEAVY ? 1.4 : 1,
                            comboRemaining > 0 ? 6 : fish.attackInterval())) {
                finishRush(true);
                return;
            }
        }
        if (--remaining <= 0 || wall.getType() != HitResult.Type.MISS || fish.horizontalCollision) {
            finishRush(false);
            return;
        }
        fish.setDeltaMovement(motion);
        fish.hasImpulse = true;
    }

    private void finishRush(boolean hit) {
        holdPosition();
        if (comboRemaining > 0) {
            comboRemaining--;
            fish.setAttackPhase(MaterialPufferfish.COMBO_GAP);
            remaining = 6;
        } else recover(fish.kind() == Kind.HEAVY && !hit);
    }

    private void recover(boolean exposed) {
        fish.setAttackPhase(exposed ? MaterialPufferfish.RECOVERY : MaterialPufferfish.REST);
        remaining = fish.kind() == Kind.HEAVY ? 30 : fish.kind() == Kind.DIAMOND ? 20 : 12;
        fish.setPuffState(exposed ? 0 : 1);
    }
}
