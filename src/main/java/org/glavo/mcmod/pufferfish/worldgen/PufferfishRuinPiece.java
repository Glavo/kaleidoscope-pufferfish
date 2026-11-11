// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.glavo.mcmod.pufferfish.AncientPrismarineBlockEntity;
import org.glavo.mcmod.pufferfish.PufferfishBlocks;

public final class PufferfishRuinPiece extends StructurePiece {
    private final boolean ancient;
    private final long decorationSeed;

    public PufferfishRuinPiece(BlockPos floor, boolean ancient, long decorationSeed) {
        super(PufferfishRuins.PIECE.get(), 0, new BoundingBox(floor.getX() - 4, floor.getY() - 4, floor.getZ() - 4,
                floor.getX() + 4, floor.getY() + 6, floor.getZ() + 4));
        this.ancient = ancient;
        this.decorationSeed = decorationSeed;
        setOrientation(Direction.SOUTH);
    }

    public PufferfishRuinPiece(CompoundTag tag) {
        super(PufferfishRuins.PIECE.get(), tag);
        ancient = tag.getBoolean("Ancient");
        decorationSeed = tag.getLong("DecorationSeed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("Ancient", ancient);
        tag.putLong("DecorationSeed", decorationSeed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random,
            BoundingBox box, ChunkPos chunk, BlockPos origin) {
        var decoration = RandomSource.create(decorationSeed);
        var stone = ancient ? PufferfishBlocks.ANCIENT_PRISMARINE.get() : Blocks.PRISMARINE;
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) {
            if (Math.abs(x - 4) + Math.abs(z - 4) > 6) continue;
            for (int y = 0; y < 4; y++) {
                var existing = level.getBlockState(getWorldPos(x, y, z));
                if (existing.isAir() || !existing.getFluidState().isEmpty()) {
                    placeBlock(level, stone.defaultBlockState(), x, y, z, box);
                }
            }
            var floor = ancient || (x + z) % 3 == 0 ? stone : Blocks.PRISMARINE_BRICKS;
            placeBlock(level, floor.defaultBlockState(), x, 4, z, box);
        }
        for (int x = 2; x <= 6; x++) for (int y = 6; y <= 10; y++) for (int z = 2; z <= 6; z++) {
            int dx = Math.abs(x - 4), dy = Math.abs(y - 8), dz = Math.abs(z - 4);
            boolean frame = dx == 0 && (dy == 2 || dz == 2) || dy == 0 && (dx == 2 || dz == 2)
                    || dz == 0 && (dx == 2 || dy == 2);
            var block = !frame ? Blocks.WATER : ancient ? stone : switch (decoration.nextInt(3)) {
                case 0 -> Blocks.PRISMARINE;
                case 1 -> Blocks.PRISMARINE_BRICKS;
                default -> Blocks.DARK_PRISMARINE;
            };
            placeBlock(level, block.defaultBlockState(), x, y, z, box);
        }
        for (int x : new int[] {1, 7}) for (int z : new int[] {1, 7}) {
            int height = 1 + decoration.nextInt(3);
            for (int y = 5; y < 5 + height; y++) {
                placeBlock(level, (ancient ? stone : Blocks.DARK_PRISMARINE).defaultBlockState(), x, y, z, box);
            }
            placeBlock(level, Blocks.SEA_LANTERN.defaultBlockState(), x, 5 + height, z, box);
        }
        if (ancient) {
            for (var offset : new int[][] {{1, 4, 4}, {7, 4, 4}, {2, 8, 4}, {6, 8, 4}}) {
                placeBlock(level, PufferfishBlocks.SUSPICIOUS_ANCIENT_PRISMARINE.get().defaultBlockState(), offset[0], offset[1], offset[2], box);
                var treasure = getWorldPos(offset[0], offset[1], offset[2]);
                long lootSeed = decoration.nextLong();
                if (box.isInside(treasure) && level.getBlockEntity(treasure) instanceof AncientPrismarineBlockEntity entity) {
                    entity.setLootTable(AncientPrismarineBlockEntity.LOOT_TABLE, lootSeed);
                }
            }
        }
        var core = ancient ? PufferfishBlocks.ANCIENT_CONDUIT.get() : PufferfishBlocks.CONDUIT.get();
        placeBlock(level, core.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 4, 8, 4, box);
    }
}
