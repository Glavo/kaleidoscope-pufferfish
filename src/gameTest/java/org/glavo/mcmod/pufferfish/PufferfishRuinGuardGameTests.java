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
        var level = helper.getLevel();
        var floor = helper.absolutePos(new BlockPos(2, 40, 2));
        var context = StructurePieceSerializationContext.fromLevel(level);
        int diamonds = 0, total = 0;
        var copperStages = new java.util.HashSet<Integer>();
        boolean three = false, four = false;
        var seeds = RandomSource.create(74392L);
        for (int sample = 0; sample < 32; sample++) {
            long seed = seeds.nextLong();
            var piece = new PufferfishRuinPiece(floor, true, seed);
            var initial = piece.createTag(context);
            var box = piece.getBoundingBox();
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(),
                    box, new ChunkPos(floor), floor);
            var expected = new HashMap<BlockPos, EntityType<?>>();
            var guards = level.getEntitiesOfClass(MaterialPufferfish.class, AABB.of(box));
            helper.assertTrue(guards.size() >= 3 && guards.size() <= 4, "Ancient ruin must generate three or four guards");
            three |= guards.size() == 3;
            four |= guards.size() == 4;
            boolean copper = false, iron = false;
            for (var fish : guards) {
                helper.assertTrue(fish.isPersistenceRequired() && fish.getRestrictCenter().equals(floor.above(4))
                        && fish.getRestrictRadius() == 12 && fish.isHostileToPlayers(), "Guard lost its post or hostility");
                helper.assertTrue(level.isWaterAt(fish.blockPosition()) && level.noCollision(fish), "Guard spawned outside open water");
                helper.assertTrue(fish.kind() == PufferfishMobConfig.Kind.COPPER || fish.kind() == PufferfishMobConfig.Kind.IRON
                        || fish.kind() == PufferfishMobConfig.Kind.DIAMOND, "Unexpected ruin guard variant");
                copper |= fish.kind() == PufferfishMobConfig.Kind.COPPER;
                if (fish.kind() == PufferfishMobConfig.Kind.COPPER) {
                    int stage = CopperPufferfish.index(fish.fish());
                    helper.assertTrue(stage >= 1 && stage <= 3, "Copper guard spawned fresh or waxed");
                    copperStages.add(stage);
                }
                iron |= fish.kind() == PufferfishMobConfig.Kind.IRON;
                if (fish.kind() == PufferfishMobConfig.Kind.DIAMOND) diamonds++;
                total++;
                expected.put(fish.blockPosition(), fish.getType());
                fish.discard();
            }
            helper.assertTrue(copper && iron, "Ancient ruin lacks its copper or iron guard");
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
        helper.assertTrue(three && four && diamonds > 0 && diamonds < total / 5, "Guard counts or rare diamond variants did not vary");
        helper.assertTrue(copperStages.size() == 3, "An aged copper guard variant is missing");
        var normal = new PufferfishRuinPiece(floor, false, 74392L);
        normal.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(),
                normal.getBoundingBox(), new ChunkPos(floor), floor);
        helper.assertTrue(level.getEntitiesOfClass(MaterialPufferfish.class, AABB.of(normal.getBoundingBox())).isEmpty(),
                "Ordinary ruin generated guards");
        var box = normal.getBoundingBox();
        for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            level.removeBlock(pos, false);
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
