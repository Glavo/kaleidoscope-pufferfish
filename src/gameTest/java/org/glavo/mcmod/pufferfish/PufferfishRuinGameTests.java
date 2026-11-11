// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.worldgen.PufferfishRuinPiece;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishRuinGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void vanillaOverworldTerrainProducesNaturalRuinStarts(GameTestHelper helper) {
        var level = helper.getLevel();
        var registries = level.registryAccess();
        long seed = 74392L;
        var preset = ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, ResourceLocation.withDefaultNamespace("overworld"));
        var biomes = MultiNoiseBiomeSource.createFromPreset(registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(preset));
        var settings = registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var generator = new NoiseBasedChunkGenerator(biomes, settings);
        var noise = RandomState.create(settings.value(), registries.lookupOrThrow(Registries.NOISE), seed);
        var state = generator.createState(registries.lookupOrThrow(Registries.STRUCTURE_SET), noise, seed);
        var structures = registries.registryOrThrow(Registries.STRUCTURE);
        var normal = structures.get(PufferfishPlease.id("pufferfish_ruin"));
        var ancient = structures.get(PufferfishPlease.id("ancient_pufferfish_ruin"));
        var placement = (RandomSpreadStructurePlacement) registries.registryOrThrow(Registries.STRUCTURE_SET)
                .get(PufferfishPlease.id("pufferfish_ruins")).placement();
        for (int regionX = -4; regionX <= 4; regionX++) for (int regionZ = -4; regionZ <= 4; regionZ++) {
            var chunk = placement.getPotentialStructureChunk(seed, regionX * placement.spacing(), regionZ * placement.spacing());
            if (!placement.isStructureChunk(state, chunk.x, chunk.z)) continue;
            var biome = biomes.getNoiseBiome(chunk.getMiddleBlockX() >> 2, 8, chunk.getMiddleBlockZ() >> 2, noise.sampler());
            if (!normal.biomes().contains(biome)) continue;
            var start = normal.generate(registries, generator, biomes, noise, level.getServer().getStructureManager(),
                    seed, chunk, 0, level, normal.biomes()::contains);
            if (!start.isValid()) continue;
            helper.assertTrue(ancient.generate(registries, generator, biomes, noise, level.getServer().getStructureManager(),
                    seed, chunk, 0, level, ancient.biomes()::contains).isValid(), "Ancient ruin rejected a valid ocean site");
            helper.assertTrue(start.getBoundingBox().maxY() < generator.getSeaLevel(), "Natural ruin protrudes above sea level");
            helper.succeed();
            return;
        }
        helper.fail("No natural ruin start was found in the seeded ocean sample");
    }

    @GameTest(template = "empty")
    public static void oceanGenerationRequiresDepthAndUsesBothRareVariants(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var structures = registries.registryOrThrow(Registries.STRUCTURE);
        for (String name : List.of("pufferfish_ruin", "ancient_pufferfish_ruin")) {
            var structure = structures.get(PufferfishPlease.id(name));
            helper.assertTrue(structure != null, "Missing ocean ruin structure: " + name);
            var start = generate(helper, structure, Biomes.OCEAN, 30);
            helper.assertTrue(start.isValid() && start.getPieces().size() == 1, "Deep ocean rejected the ruin");
            var context = StructurePieceSerializationContext.fromLevel(helper.getLevel());
            var saved = start.createTag(context, start.getChunkPos());
            var restored = StructureStart.loadStaticStart(context, saved, 74392L);
            helper.assertTrue(restored != null && restored.isValid()
                    && restored.getPieces().getFirst().createTag(context).equals(start.getPieces().getFirst().createTag(context)),
                    "Ruin variant or decoration seed changed after loading");
            helper.assertTrue(!generate(helper, structure, Biomes.OCEAN, 4).isValid(), "Shallow water accepted a ruin");
            helper.assertTrue(!generate(helper, structure, Biomes.OCEAN, 0).isValid(), "Dry terrain accepted a ruin");
            helper.assertTrue(!generate(helper, structure, Biomes.PLAINS, 30).isValid(), "Non-ocean biome accepted a ruin");
        }
        var set = registries.registryOrThrow(Registries.STRUCTURE_SET).get(PufferfishPlease.id("pufferfish_ruins"));
        helper.assertTrue(set != null && set.structures().size() == 2, "Both ruin variants must share one placement grid");
        helper.assertTrue(set.placement() instanceof RandomSpreadStructurePlacement placement
                && placement.spacing() == 48 && placement.separation() == 24, "Ocean ruins are not sparsely spaced");
        helper.assertTrue(set.structures().get(0).weight() == 3 && set.structures().get(1).weight() == 1,
                "Ancient ruins should be rarer than ordinary ruins");
        helper.succeed();
    }

    private static StructureStart generate(GameTestHelper helper, Structure structure, ResourceKey<Biome> biome, int waterDepth) {
        var level = helper.getLevel();
        var settings = new FlatLevelGeneratorSettings(Optional.empty(),
                level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome), List.of());
        settings.getLayersInfo().add(new FlatLayerInfo(80, Blocks.STONE));
        if (waterDepth > 0) settings.getLayersInfo().add(new FlatLayerInfo(waterDepth, Blocks.WATER));
        settings.updateLayers();
        var generator = new FlatLevelSource(settings);
        return structure.generate(level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
                level.getServer().getStructureManager(), 74392L, new ChunkPos(0, 0), 0, level, structure.biomes()::contains);
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void generatedAltarsSeparateMaterialsAndAncientRewards(GameTestHelper helper) {
        var level = helper.getLevel();
        var pieces = new ArrayList<PufferfishRuinPiece>();
        var cores = new ArrayList<BlockPos>();
        var frames = new ArrayList<List<BlockPos>>();
        var treasures = new ArrayList<BlockPos>();
        for (boolean ancient : new boolean[] {false, true}) {
            var floor = helper.absolutePos(new BlockPos(2, ancient ? 54 : 30, 2));
            var piece = new PufferfishRuinPiece(floor, ancient, 74392L);
            pieces.add(piece);
            var box = piece.getBoundingBox();
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(),
                    box, new ChunkPos(floor), floor);
            var expectedFrame = new ArrayList<BlockPos>();
            PufferfishConduitBlockEntity.updateFrame(level, floor.above(4), expectedFrame);
            var context = StructurePieceSerializationContext.fromLevel(level);
            var restored = new PufferfishRuinPiece(piece.createTag(context));
            int split = floor.getX();
            for (var clip : List.of(new BoundingBox(box.minX(), box.minY(), box.minZ(), split, box.maxY(), box.maxZ()),
                    new BoundingBox(split + 1, box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))) {
                restored.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(),
                        clip, new ChunkPos(floor), floor);
            }
            var corePos = floor.above(4);
            cores.add(corePos);
            helper.assertTrue(level.getBlockState(corePos).is(ancient ? PufferfishBlocks.ANCIENT_CONDUIT.get() : PufferfishBlocks.CONDUIT.get()),
                    "Ruin placed the wrong conduit");
            var frame = new ArrayList<BlockPos>();
            helper.assertTrue(PufferfishConduitBlockEntity.updateFrame(level, corePos, frame), "Generated altar cannot activate");
            helper.assertTrue(ancient ? frame.size() >= 16 && frame.size() <= 27 : frame.size() == 42,
                    "Generated altar has the wrong frame completeness");
            helper.assertTrue(frame.equals(expectedFrame), "Reloaded or split generation changed the damaged frame");
            frames.add(frame);
            if (ancient) {
                helper.assertTrue(frame.stream().allMatch(pos -> level.getBlockState(pos).is(PufferfishConduitBlockEntity.ANCIENT_FRAME))
                        && frame.stream().anyMatch(pos -> level.getBlockState(pos).is(PufferfishBlocks.SUSPICIOUS_ANCIENT_PRISMARINE)),
                        "Ancient frame must mix ancient stone with brushable variants");
                for (var treasure : List.of(floor.west(3), floor.east(3), corePos.west(2), corePos.east(2))) {
                    treasures.add(treasure);
                    helper.assertTrue(level.getBlockEntity(treasure) instanceof AncientPrismarineBlockEntity, "Missing archaeology block");
                    var tag = level.getBlockEntity(treasure).saveWithFullMetadata(level.registryAccess());
                    helper.assertTrue(tag.getString("LootTable").equals(AncientPrismarineBlockEntity.LOOT_TABLE.location().toString()),
                            "Generated archaeology has no treasure table");
                }
            } else {
                for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                    var block = level.getBlockState(pos);
                    helper.assertTrue(block.isAir() || block.is(Blocks.WATER) || block.is(Blocks.PRISMARINE)
                            || block.is(Blocks.PRISMARINE_BRICKS) || block.is(Blocks.DARK_PRISMARINE)
                            || block.is(Blocks.SEA_LANTERN) || block.is(PufferfishBlocks.CONDUIT),
                            "Ordinary ruin contains ancient stone or reward blocks");
                }
            }
        }
        helper.runAfterDelay(45, () -> {
            for (var pos : cores) {
                helper.assertTrue(level.getBlockEntity(pos) instanceof PufferfishConduitBlockEntity core && core.isActive(),
                        "Generated conduit did not activate on its own");
            }
            var ancientPos = cores.get(1);
            int radius = frames.get(1).size() < 21 ? 32 : 48;
            helper.assertTrue(PufferfishConduitSpawning.isInAncientRange(level, ancientPos.above(radius - 1))
                    && !PufferfishConduitSpawning.isInAncientRange(level, ancientPos.above(radius)),
                    "Damaged ancient frame did not limit its natural aura range");
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            for (var pos : treasures) {
                var treasure = (AncientPrismarineBlockEntity) level.getBlockEntity(pos);
                for (int i = 0; i < 10; i++) treasure.brush(level.getGameTime() + i * 10, player, Direction.UP);
                helper.assertTrue(level.getBlockState(pos).is(PufferfishBlocks.ANCIENT_PRISMARINE), "Brushing did not restore ancient stone");
            }
            helper.runAfterDelay(45, () -> {
                for (int i = 0; i < cores.size(); i++) {
                    var pos = cores.get(i);
                    helper.assertTrue(((PufferfishConduitBlockEntity) level.getBlockEntity(pos)).isActive(), "Brushing deactivated the core");
                    var frame = new ArrayList<BlockPos>();
                    PufferfishConduitBlockEntity.updateFrame(level, pos, frame);
                    helper.assertTrue(frame.equals(frames.get(i)), "Brushing changed the effective frame");
                }
                helper.assertTrue(PufferfishConduitSpawning.isInAncientRange(level, ancientPos.above(radius - 1))
                        && !PufferfishConduitSpawning.isInAncientRange(level, ancientPos.above(radius)),
                        "Brushing changed the ancient aura range");
                for (var piece : pieces) {
                    var box = piece.getBoundingBox();
                    for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                        level.removeBlock(pos, false);
                    }
                }
                helper.succeed();
            });
        });
    }
}
