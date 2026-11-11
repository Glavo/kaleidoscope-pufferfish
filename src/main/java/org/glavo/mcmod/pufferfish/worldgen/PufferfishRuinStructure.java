// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/// A submerged altar on a gently sloping ocean floor, with a normal or ancient conduit.
public final class PufferfishRuinStructure extends Structure {
    public static final MapCodec<PufferfishRuinStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance), Codec.BOOL.fieldOf("ancient").forGetter(structure -> structure.ancient)
    ).apply(instance, PufferfishRuinStructure::new));
    private final boolean ancient;

    public PufferfishRuinStructure(StructureSettings settings, boolean ancient) {
        super(settings);
        this.ancient = ancient;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int dx = -4; dx <= 4; dx += 4) for (int dz = -4; dz <= 4; dz += 4) {
            int floor = context.chunkGenerator().getFirstOccupiedHeight(x + dx, z + dz, Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState());
            lowest = Math.min(lowest, floor);
            highest = Math.max(highest, floor);
        }
        if (highest - lowest > 4 || lowest <= context.heightAccessor().getMinBuildHeight() + 4) return Optional.empty();
        for (int dx = -4; dx <= 4; dx += 4) for (int dz = -4; dz <= 4; dz += 4) {
            var column = context.chunkGenerator().getBaseColumn(x + dx, z + dz, context.heightAccessor(), context.randomState());
            for (int y = highest + 1; y <= highest + 7; y++) {
                if (!column.getBlock(y).getFluidState().is(FluidTags.WATER)) return Optional.empty();
            }
        }
        var floor = new BlockPos(x, highest, z);
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(floor, builder -> builder.addPiece(new PufferfishRuinPiece(floor, ancient, seed))));
    }

    @Override
    public StructureType<?> type() { return PufferfishRuins.TYPE.get(); }
}
