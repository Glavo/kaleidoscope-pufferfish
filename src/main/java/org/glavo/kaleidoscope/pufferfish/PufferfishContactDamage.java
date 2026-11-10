// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class PufferfishContactDamage {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, KaleidoscopePufferfish.id("pufferfish_prick"));

    private PufferfishContactDamage() {}

    /// Applies contact damage and status effects once per second, including walls and ceilings.
    public static void tick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide || !entity.isAlive() || entity.isSpectator() || entity.tickCount % 20 != 0) return;
        var level = entity.level();
        var bounds = entity.getBoundingBox().inflate(0.001);
        boolean touching = false;
        var effects = new java.util.HashMap<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>, MobEffectInstance>();
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PufferfishSkinBlock) && !(state.getBlock() instanceof PlacedPufferfishBlock)) continue;
            for (var shape : state.getCollisionShape(level, pos, CollisionContext.of(entity)).toAabbs()) {
                if (shape.move(pos).intersects(bounds)) {
                    touching = true;
                    if (entity instanceof LivingEntity) {
                        var effect = poison(level, pos, state);
                        var poison = effect == null ? null : effects.get(effect.getEffect());
                        if (effect != null && (poison == null || effect.getAmplifier() > poison.getAmplifier()
                                || effect.getAmplifier() == poison.getAmplifier() && effect.getDuration() > poison.getDuration())) effects.put(effect.getEffect(), effect);
                    }
                    break;
                }
            }
        }
        if (!touching) return;
        var source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE));
        entity.hurt(source, 1);
        if (entity instanceof LivingEntity living && living.isAlive() && !living.isInvulnerableTo(source)) {
            for (var effect : effects.values()) {
                var current = living.getEffect(effect.getEffect());
                if (current == null || !current.isInfiniteDuration() && (current.getAmplifier() < effect.getAmplifier()
                        || current.getAmplifier() == effect.getAmplifier() && current.getDuration() <= effect.getDuration() / 2)) {
                    living.addEffect(effect);
                }
            }
        }
    }

    @javax.annotation.Nullable
    private static MobEffectInstance poison(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof PufferfishSkinBlock) {
            if (CopperPufferfish.skinIndex(state.getBlock().asItem().getDefaultInstance()) >= 4) return null;
            boolean radiant = state.is(PufferfishBlocks.RADIANT_SKIN.get());
            return new MobEffectInstance(radiant ? MobEffects.WITHER : MobEffects.POISON, radiant ? PufferfishCombat.RADIANT_WITHER_DURATION_TICKS : 120, radiant ? 1 : 0);
        }
        if (level.getBlockEntity(pos) instanceof PlacedPufferfishBlockEntity placed && !placed.fish().isEmpty()) {
            return PufferfishCombat.passiveEffect(placed.fish());
        }
        return null;
    }
}
