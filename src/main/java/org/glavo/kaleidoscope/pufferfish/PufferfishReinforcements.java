// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.EventHooks;
import org.glavo.kaleidoscope.pufferfish.entity.MaterialPufferfish;

public final class PufferfishReinforcements {
    private PufferfishReinforcements() {}

    public static boolean summon(MaterialPufferfish caller, LivingEntity attacker) {
        if (!(caller.level() instanceof ServerLevel level)) return false;
        int radius = PufferfishMobConfig.value(PufferfishMobConfig.REINFORCEMENT_RADIUS);
        int limit = PufferfishMobConfig.value(PufferfishMobConfig.REINFORCEMENT_NEARBY_LIMIT);
        if (level.getEntitiesOfClass(MaterialPufferfish.class, caller.getBoundingBox().inflate(radius),
                fish -> fish != caller && fish.isAlive()).size() >= limit) return false;
        int total = PufferfishMobConfig.REINFORCEMENT_WEIGHTS.values().stream().mapToInt(PufferfishMobConfig::value).sum();
        if (total == 0) return false;
        int roll = caller.getRandom().nextInt(total);
        PufferfishMobConfig.Kind selected = null;
        for (var entry : PufferfishMobConfig.REINFORCEMENT_WEIGHTS.entrySet()) {
            roll -= PufferfishMobConfig.value(entry.getValue());
            if (roll < 0) { selected = entry.getKey(); break; }
        }
        var variant = PufferfishMobs.VARIANTS.get(selected.name().toLowerCase(java.util.Locale.ROOT) + "_pufferfish");
        var reinforcement = variant.entity().get().create(level);
        if (reinforcement == null) return false;
        for (int attempt = 0; attempt < 48; attempt++) {
            int dx = caller.getRandom().nextInt(radius * 2 + 1) - radius;
            int dz = caller.getRandom().nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz < 4 || dx * dx + dz * dz > radius * radius) continue;
            var pos = caller.blockPosition().offset(dx, caller.getRandom().nextInt(5) - 2, dz);
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)) continue;
            reinforcement.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            reinforcement.setPuffState(1);
            var bounds = reinforcement.getBoundingBox();
            boolean water = BlockPos.betweenClosedStream(bounds).allMatch(block -> level.getFluidState(block).is(FluidTags.WATER));
            if (!water || !level.noCollision(reinforcement) || !level.isUnobstructed(reinforcement)) continue;
            EventHooks.finalizeMobSpawn(reinforcement, level, level.getCurrentDifficultyAt(pos), MobSpawnType.REINFORCEMENT, null);
            if (reinforcement.isSpawnCancelled()) return false;
            reinforcement.joinReinforcement(attacker);
            if (!level.addFreshEntity(reinforcement)) return false;
            level.sendParticles(ParticleTypes.BUBBLE, reinforcement.getX(), reinforcement.getY(0.5), reinforcement.getZ(), 16, 0.3, 0.3, 0.3, 0.05);
            caller.playSound(SoundEvents.PUFFER_FISH_BLOW_UP, 1, 0.6F);
            return true;
        }
        return false;
    }
}
