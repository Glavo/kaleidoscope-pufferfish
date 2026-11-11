// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.HashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;
import org.glavo.mcmod.pufferfish.worldgen.PufferfishRuinPiece;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishRuinGuardGameTests {
    @GameTest(template = "empty")
    public static void ancientGuardsGenerateOnceAcrossChunksAndReloads(GameTestHelper helper) {
        checkRuinFishGeneration(helper, true);
    }

    @GameTest(template = "empty")
    public static void ordinaryFishGenerateOnceAcrossChunksAndReloads(GameTestHelper helper) {
        checkRuinFishGeneration(helper, false);
    }

    private static void checkRuinFishGeneration(GameTestHelper helper, boolean ancient) {
        var level = helper.getLevel();
        var floor = helper.absolutePos(new BlockPos(2, 40, 2));
        var context = StructurePieceSerializationContext.fromLevel(level);
        int diamonds = 0, golden = 0, total = 0;
        var copperStages = new java.util.HashSet<Integer>();
        var counts = new java.util.HashSet<Integer>();
        var seeds = RandomSource.create(74392L);
        for (int sample = 0; sample < 32; sample++) {
            long seed = seeds.nextLong();
            var piece = new PufferfishRuinPiece(floor, ancient, seed);
            var initial = piece.createTag(context);
            var box = piece.getBoundingBox();
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(),
                    box, new ChunkPos(floor), floor);
            var expected = new HashMap<BlockPos, EntityType<?>>();
            var guards = level.getEntitiesOfClass(MaterialPufferfish.class, AABB.of(box));
            helper.assertTrue(guards.size() >= (ancient ? 3 : 1) && guards.size() <= 4, "Unexpected ruin fish count");
            counts.add(guards.size());
            boolean copper = false, iron = false;
            for (var fish : guards) {
                helper.assertTrue(fish.isPersistenceRequired() && fish.getRestrictCenter().equals(floor.above(4))
                        && fish.getRestrictRadius() == 12 && fish.isHostileToPlayers() == ancient, "Ruin fish lost its home or temperament");
                helper.assertTrue(level.isWaterAt(fish.blockPosition()) && level.noCollision(fish), "Guard spawned outside open water");
                helper.assertTrue(fish.kind() == PufferfishMobConfig.Kind.COPPER || (ancient
                        ? fish.kind() == PufferfishMobConfig.Kind.IRON || fish.kind() == PufferfishMobConfig.Kind.DIAMOND
                        : fish.kind() == PufferfishMobConfig.Kind.GOLDEN), "Unexpected ruin fish variant");
                copper |= fish.kind() == PufferfishMobConfig.Kind.COPPER;
                if (fish.kind() == PufferfishMobConfig.Kind.COPPER) {
                    int stage = CopperPufferfish.index(fish.fish());
                    helper.assertTrue(ancient ? stage >= 1 && stage <= 3 : stage == 0, "Ruin copper fish has the wrong oxidation stage");
                    copperStages.add(stage);
                }
                iron |= fish.kind() == PufferfishMobConfig.Kind.IRON;
                if (fish.kind() == PufferfishMobConfig.Kind.DIAMOND) diamonds++;
                if (fish.kind() == PufferfishMobConfig.Kind.GOLDEN) golden++;
                total++;
                expected.put(fish.blockPosition(), fish.getType());
                fish.discard();
            }
            if (ancient) helper.assertTrue(copper && iron, "Ancient ruin lacks its copper or iron guard");
            piece = new PufferfishRuinPiece(initial);
            for (var clip : List.of(new BoundingBox(floor.getX() + 1, box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()),
                    new BoundingBox(box.minX(), box.minY(), box.minZ(), floor.getX(), box.maxY(), box.maxZ()))) {
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(),
                        clip, new ChunkPos(floor), floor);
                piece = new PufferfishRuinPiece(piece.createTag(context));
            }
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(),
                    box, new ChunkPos(floor), floor);
            guards = level.getEntitiesOfClass(MaterialPufferfish.class, AABB.of(box));
            helper.assertTrue(guards.size() == expected.size(), "Split generation or reload duplicated guards");
            for (var fish : guards) {
                helper.assertTrue(expected.get(fish.blockPosition()) == fish.getType(), "Split generation changed guard positions or variants");
                fish.discard();
            }
            piece = new PufferfishRuinPiece(piece.createTag(context));
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(),
                    box, new ChunkPos(floor), floor);
            helper.assertTrue(level.getEntitiesOfClass(MaterialPufferfish.class, AABB.of(box)).isEmpty(), "Cleared guards respawned");
            for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                level.removeBlock(pos, false);
            }
        }
        helper.assertTrue(counts.size() == (ancient ? 2 : 4), "Ruin fish counts did not vary across the full range");
        if (ancient) {
            helper.assertTrue(diamonds > 0 && diamonds < total / 5, "Rare diamond guards are missing or too common");
            helper.assertTrue(copperStages.size() == 3, "An aged copper guard variant is missing");
        } else {
            helper.assertTrue(golden > 0 && golden < total / 2, "Ordinary ruins lack golden fish or favor them over copper");
        }
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void ordinaryResidentsStayNeutralAcrossSavingAndCapture(GameTestHelper helper) {
        var level = helper.getLevel();
        var home = helper.absolutePos(new BlockPos(2, 3, 7));
        level.setBlock(home.east(), Blocks.WATER.defaultBlockState(), 2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(home.east().getCenter());
        player.tick();
        for (String variant : List.of("copper_pufferfish", "golden_pufferfish")) {
            var type = PufferfishMobs.VARIANTS.get(variant).entity().get();
            var fish = type.create(level);
            fish.setPos(home.getCenter());
            fish.setHome(home);
            var loaded = type.create(level);
            loaded.load(fish.saveWithoutId(new CompoundTag()));
            helper.assertTrue(loaded.isPersistenceRequired() && loaded.getRestrictCenter().equals(home)
                    && loaded.getRestrictRadius() == 12 && !loaded.isHostileToPlayers() && !loaded.validTarget(player),
                    "Ordinary resident lost its home or became hostile after loading");
            var bucket = loaded.getBucketItemStack();
            loaded.saveToBucketTag(bucket);
            var released = type.create(level);
            released.loadFromBucketTag(bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
            helper.assertTrue(!released.hasRestriction() && !released.isHostileToPlayers(), "Captured resident retained its home or became hostile");
        }
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void guardsProtectTheirPostPersistAndBecomeOrdinaryFishWhenCaptured(GameTestHelper helper) {
        var level = helper.getLevel();
        var home = helper.absolutePos(new BlockPos(2, 3, 7));
        for (var pos : BlockPos.betweenClosed(home.offset(-1, -1, -2), home.offset(14, 2, 2))) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(home.east(2).getCenter());
        player.setNoGravity(true);
        player.tick();
        level.addFreshEntity(player);
        var fish = PufferfishMobs.VARIANTS.get("copper_pufferfish").entity().get().create(level);
        fish.setPos(home.getCenter());
        helper.assertTrue(!fish.validTarget(player), "Ordinary copper fish hunted players");
        fish.guard(home);
        helper.assertTrue(fish.validTarget(player) && fish.findCombatTarget() == player, "Copper guard did not defend its post");
        var loaded = PufferfishMobs.VARIANTS.get("copper_pufferfish").entity().get().create(level);
        loaded.load(fish.saveWithoutId(new CompoundTag()));
        helper.assertTrue(loaded.isPersistenceRequired() && loaded.getRestrictCenter().equals(home) && loaded.validTarget(player),
                "Reloading lost guard behavior");
        loaded.setPos(home.east(11).getCenter());
        player.setPos(home.east(13).getCenter());
        player.tick();
        helper.assertTrue(player.isInWater() && loaded.distanceToSqr(player) < 16 && !loaded.validTarget(player),
                "Guard pursued a nearby player outside its post radius");
        var bucket = loaded.getBucketItemStack();
        loaded.saveToBucketTag(bucket);
        var released = PufferfishMobs.VARIANTS.get("copper_pufferfish").entity().get().create(level);
        released.loadFromBucketTag(bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
        helper.assertTrue(!released.hasRestriction() && !released.isHostileToPlayers(), "Captured fish retained its guard post");
        player.discard();
        helper.succeed();
    }
}
