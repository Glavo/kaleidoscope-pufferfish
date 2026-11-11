// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

public final class AncientPrismarineBlockEntity extends BrushableBlockEntity {
    public static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            PufferfishPlease.id("archaeology/ancient_prismarine"));

    public AncientPrismarineBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        setLootTable(LOOT_TABLE, 0);
    }

    @Override
    public BlockEntityType<?> getType() { return PufferfishBlocks.ANCIENT_PRISMARINE_ENTITY.get(); }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // Resolved treasure must survive reloading without rolling the default table again.
        setLootTable(null, 0);
        super.loadAdditional(tag, registries);
    }
}
