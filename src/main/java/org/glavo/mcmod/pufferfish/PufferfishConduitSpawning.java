// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

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
import org.jetbrains.annotations.Nullable;

public final class PufferfishConduitSpawning {
    public static final TagKey<EntityType<?>> REPLACED = TagKey.create(Registries.ENTITY_TYPE,
            PufferfishPlease.id("conduit_replaced_spawns"));
    public static final TagKey<EntityType<?>> SUPPRESSED = TagKey.create(Registries.ENTITY_TYPE,
            PufferfishPlease.id("conduit_suppressed_spawns"));
    private static final Map<Level, Map<BlockPos, Aura>> ACTIVE = new WeakHashMap<>();
    private record Aura(int radius, boolean ancient) {}
    // NaturalSpawner checks candidate identity again before spawning the selected mob.
    private static final MobSpawnSettings.SpawnerData PUFFERFISH = new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 1, 1, 3);

    private PufferfishConduitSpawning() {}

    /// Registers the current server-side range, or removes it when the core is inactive or unloaded.
    public static void update(@Nullable Level level, BlockPos pos, int radius) {
        if (level == null || level.isClientSide) return;
        if (radius > 0) {
            ACTIVE.computeIfAbsent(level, ignored -> new HashMap<>()).put(pos.immutable(),
                    new Aura(radius, level.getBlockState(pos).is(PufferfishBlocks.ANCIENT_CONDUIT.get())));
        } else {
            @Nullable var positions = ACTIVE.get(level);
            if (positions != null) {
                positions.remove(pos);
                if (positions.isEmpty()) ACTIVE.remove(level);
            }
        }
    }

    public static void unload(LevelEvent.Unload event) {
        ACTIVE.remove(event.getLevel());
    }

    /// Uses the same spherical range for spawn replacement and material-fish rarity.
    public static boolean isInRange(Level level, BlockPos pos) {
        var positions = ACTIVE.get(level);
        return positions != null && positions.entrySet().stream()
                .anyMatch(entry -> entry.getKey().closerThan(pos, entry.getValue().radius()));
    }

    public static boolean isInAncientRange(Level level, BlockPos pos) {
        var positions = ACTIVE.get(level);
        return positions != null && positions.entrySet().stream().anyMatch(entry -> entry.getValue().ancient()
                && entry.getKey().closerThan(pos, entry.getValue().radius()));
    }

    public static void potentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !ACTIVE.containsKey(level)) return;
        var candidates = event.getSpawnerDataList();
        if (candidates.stream().noneMatch(entry -> entry.type != EntityType.PUFFERFISH
                && (entry.type.is(SUPPRESSED) || entry.type.is(REPLACED))) || !isInRange(level, event.getPos())) return;
        boolean replaced = false;
        for (var entry : List.copyOf(candidates)) {
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
