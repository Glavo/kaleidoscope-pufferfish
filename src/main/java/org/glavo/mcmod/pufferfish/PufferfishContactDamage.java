// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

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
            Registries.DAMAGE_TYPE, PufferfishPlease.id("pufferfish_prick"));

    private PufferfishContactDamage() {}

    /// Applies contact damage and status effects once per second, including walls and ceilings.
    public static void tick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide || !entity.isAlive() || entity.isSpectator() || entity.tickCount % 20 != 0) return;
        var level = entity.level();
        var bounds = entity.getBoundingBox().inflate(0.001);
        boolean touching = false;
        PufferfishPoison poison = null;
        boolean radiant = false;
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PufferfishSkinBlock) && !(state.getBlock() instanceof PlacedPufferfishBlock)) continue;
            for (var shape : state.getCollisionShape(level, pos, CollisionContext.of(entity)).toAabbs()) {
                if (shape.move(pos).intersects(bounds)) {
                    touching = true;
                    if (entity instanceof LivingEntity) {
                        var stack = contactItem(level, pos, state);
                        poison = PufferfishPoison.strongest(poison, PufferfishPoison.of(stack));
                        radiant |= PufferfishItems.isRadiant(stack);
                    }
                    break;
                }
            }
        }
        if (!touching) return;
        var source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE));
        if (!entity.hurt(source, 1)) return;
        if (entity instanceof LivingEntity living && living.isAlive() && !living.isInvulnerableTo(source)) {
            if (poison != null && poison.shouldRefresh(living)) poison.apply(living, null, PufferfishCombat.STATUS_EFFECT_DURATION_TICKS);
            if (radiant) {
                PufferfishCombat.applyRadiantGlow(living, null);
                var current = living.getEffect(MobEffects.WITHER);
                if (current == null || !current.isInfiniteDuration() && current.getAmplifier() == 0
                        && current.getDuration() <= PufferfishCombat.STATUS_EFFECT_DURATION_TICKS / 2) {
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, PufferfishCombat.STATUS_EFFECT_DURATION_TICKS));
                }
            }
        }
    }

    private static net.minecraft.world.item.ItemStack contactItem(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof PufferfishSkinBlock) {
            return state.getBlock().asItem().getDefaultInstance();
        }
        if (level.getBlockEntity(pos) instanceof PlacedPufferfishBlockEntity placed && !placed.fish().isEmpty()) {
            return placed.fish();
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
