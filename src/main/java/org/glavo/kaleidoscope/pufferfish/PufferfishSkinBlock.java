// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/// A rotatable skin that lies on floors or attaches to sturdy walls and ceilings.
public final class PufferfishSkinBlock extends FaceAttachedHorizontalDirectionalBlock {
    /// Serializes skin blocks using their block properties.
    public static final MapCodec<PufferfishSkinBlock> CODEC = simpleCodec(PufferfishSkinBlock::new);

    /// One-pixel-thick shapes indexed by the outward normal of the attached surface.
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP, Block.box(0, 0, 0, 16, 1, 16),
            Direction.DOWN, Block.box(0, 15, 0, 16, 16, 16),
            Direction.NORTH, Block.box(0, 0, 15, 16, 16, 16),
            Direction.SOUTH, Block.box(0, 0, 0, 16, 16, 1),
            Direction.WEST, Block.box(15, 0, 0, 16, 16, 16),
            Direction.EAST, Block.box(0, 0, 0, 1, 16, 16));

    /// Creates a skin with a north-facing floor state as its default.
    public PufferfishSkinBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACE, AttachFace.FLOOR).setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<PufferfishSkinBlock> codec() {
        return CODEC;
    }

    /// Attaches to the clicked face, using the player's horizontal facing on floors and ceilings.
    /// Returns null when that surface cannot support the skin.
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var normal = context.getClickedFace();
        var face = normal == Direction.UP ? AttachFace.FLOOR
                : normal == Direction.DOWN ? AttachFace.CEILING : AttachFace.WALL;
        var facing = normal.getAxis() == Direction.Axis.Y ? context.getHorizontalDirection() : normal;
        var state = defaultBlockState().setValue(FACE, face).setValue(FACING, facing);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /// Defines the attachment surface and horizontal orientation.
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    /// Returns the thin shape against the supporting surface.
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(getConnectedDirection(state));
    }

    /// Requires non-air support below floor skins and sturdy support on other surfaces.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(FACE) == AttachFace.FLOOR
                ? !level.isEmptyBlock(pos.below()) : super.canSurvive(state, level, pos);
    }
}
