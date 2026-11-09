// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class PufferfishConduitSpawning {
    public static final TagKey<EntityType<?>> REPLACED = TagKey.create(Registries.ENTITY_TYPE,
            KaleidoscopePufferfish.id("conduit_replaced_spawns"));
    public static final TagKey<EntityType<?>> SUPPRESSED = TagKey.create(Registries.ENTITY_TYPE,
            KaleidoscopePufferfish.id("conduit_suppressed_spawns"));
    private static final Map<Level, Map<BlockPos, Integer>> ACTIVE = new WeakHashMap<>();
    // NaturalSpawner checks candidate identity again before spawning the selected mob.
    private static final MobSpawnSettings.SpawnerData PUFFERFISH = new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 1, 1, 3);

    private PufferfishConduitSpawning() {}

    static void update(Level level, BlockPos pos, int radius) {
        if (level == null || level.isClientSide) return;
        if (radius > 0) {
            ACTIVE.computeIfAbsent(level, ignored -> new HashMap<>()).put(pos.immutable(), radius);
        } else {
            var positions = ACTIVE.get(level);
            if (positions != null) {
                positions.remove(pos);
                if (positions.isEmpty()) ACTIVE.remove(level);
            }
        }
    }

    public static void unload(LevelEvent.Unload event) {
        ACTIVE.remove(event.getLevel());
    }

    public static void potentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var positions = ACTIVE.get(level);
        if (positions == null || positions.entrySet().stream()
                .noneMatch(entry -> entry.getKey().closerThan(event.getPos(), entry.getValue()))) return;
        boolean replaced = false;
        for (var entry : List.copyOf(event.getSpawnerDataList())) {
            if (entry.type == EntityType.PUFFERFISH) continue;
            if (entry.type.is(SUPPRESSED) || entry.type.is(REPLACED)) {
                event.removeSpawnerData(entry);
                replaced |= entry.type.is(REPLACED);
            }
        }
        if (replaced && event.getMobCategory() == MobCategory.WATER_AMBIENT
                && event.getSpawnerDataList().stream().noneMatch(entry -> entry.type == EntityType.PUFFERFISH)) {
            event.addSpawnerData(PUFFERFISH);
        }
    }
}
