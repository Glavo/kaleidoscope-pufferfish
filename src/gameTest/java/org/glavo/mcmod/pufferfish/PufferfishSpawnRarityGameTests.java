// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishSpawnRarityGameTests {
    private static BlockPos water(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(2, 2, 2));
        var pos = new BlockPos(origin.getX(), helper.getLevel().getSeaLevel() - 1, origin.getZ());
        for (int y = -1; y <= 1; y++) helper.getLevel().setBlockAndUpdate(pos.offset(0, y, 0), Blocks.WATER.defaultBlockState());
        return pos;
    }

    @GameTest(template = "empty")
    public static void naturalSpawnChecksUseConduitRangeWithoutStacking(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = water(helper);
        var first = pos.east(8);
        var second = pos.west(8);
        try {
            verifyNatural(helper, pos, false);
            PufferfishConduitSpawning.update(level, first, 8);
            verifyNatural(helper, pos, false);
            PufferfishConduitSpawning.update(level, first, 9);
            verifyNatural(helper, pos, true);
            helper.assertTrue(!PufferfishConduitSpawning.isInRange(level.getServer().getLevel(Level.NETHER), pos),
                    "Conduit range leaked into another dimension");
            PufferfishConduitSpawning.update(level, second, 9);
            verifyNatural(helper, pos, true);
            PufferfishConduitSpawning.update(level, first, 0);
            verifyNatural(helper, pos, true);
            PufferfishConduitSpawning.update(level, second, 0);
            verifyNatural(helper, pos, false);
        } finally {
            PufferfishConduitSpawning.update(level, first, 0);
            PufferfishConduitSpawning.update(level, second, 0);
            for (int y = -1; y <= 1; y++) level.removeBlock(pos.offset(0, y, 0), false);
        }
        helper.succeed();
    }

    private static void verifyNatural(GameTestHelper helper, BlockPos pos, boolean conduit) {
        for (var entry : PufferfishMobs.VARIANTS.entrySet()) {
            int expected = switch (entry.getKey()) {
                case "diamond_pufferfish" -> conduit ? 64 : 16;
                case "netherite_pufferfish" -> conduit ? 1024 : 64;
                case "heavy_pufferfish", "radiant_pufferfish" -> conduit ? 4096 : 256;
                case "golden_pufferfish" -> 8;
                default -> 4;
            };
            for (int roll = 0; roll <= 1; roll++) {
                var random = new SpawnRoll(expected, roll);
                boolean allowed = SpawnPlacements.checkSpawnRules(entry.getValue().entity().get(), helper.getLevel(), MobSpawnType.NATURAL, pos, random);
                helper.assertTrue(random.calls == 1 && allowed == (roll == 0), "Natural spawn rarity check failed for " + entry.getKey());
            }
        }
    }

    @GameTest(template = "empty")
    public static void bucketsAndEggsSkipRarityButKeepWaterRequirements(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = water(helper);
        var center = pos.east(8);
        PufferfishConduitSpawning.update(level, center, 9);
        try {
            for (var variant : PufferfishMobs.VARIANTS.values()) {
                for (var reason : java.util.List.of(MobSpawnType.BUCKET, MobSpawnType.SPAWN_EGG)) {
                    var random = new SpawnRoll(0, 0);
                    helper.assertTrue(SpawnPlacements.checkSpawnRules(variant.entity().get(), level, reason, pos, random)
                            && random.calls == 0, "Non-natural spawn was subjected to rarity");
                }
            }
            level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
            var variant = PufferfishMobs.VARIANTS.get("netherite_pufferfish");
            var random = new SpawnRoll(0, 0);
            helper.assertTrue(!SpawnPlacements.checkSpawnRules(variant.entity().get(), level, MobSpawnType.NATURAL, pos, random)
                    && random.calls == 0, "Conduit bypassed the water requirement");
        } finally {
            PufferfishConduitSpawning.update(level, center, 0);
            for (int y = -1; y <= 1; y++) level.removeBlock(pos.offset(0, y, 0), false);
        }
        helper.succeed();
    }

    private static final class SpawnRoll extends LegacyRandomSource {
        private final int expectedBound;
        private final int roll;
        private int calls;

        SpawnRoll(int expectedBound, int roll) {
            super(0);
            this.expectedBound = expectedBound;
            this.roll = roll;
        }

        @Override
        public int nextInt(int bound) {
            if (bound != expectedBound) throw new AssertionError("Expected spawn roll bound " + expectedBound + ", got " + bound);
            calls++;
            return roll;
        }
    }
}
