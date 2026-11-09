// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KaleidoscopePufferfish.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KaleidoscopePufferfish.MODID);
    public static final DeferredBlock<PufferfishConduitBlock> CONDUIT = BLOCKS.registerBlock("pufferfish_conduit",
            PufferfishConduitBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CONDUIT));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PufferfishConduitBlockEntity>> CONDUIT_ENTITY =
            BLOCK_ENTITIES.register("pufferfish_conduit", () -> BlockEntityType.Builder.of(PufferfishConduitBlockEntity::new, CONDUIT.get()).build(null));
    /// The placeable skin obtained from undyed pufferfish.
    private static final DeferredBlock<PufferfishSkinBlock> PLAIN_SKIN = registerSkin(null);
    /// Placeable skins indexed by the dye color of their ingredient fish.
    private static final Map<DyeColor, DeferredBlock<PufferfishSkinBlock>> COLORED_SKINS = new EnumMap<>(DyeColor.class);

    static {
        ITEMS.registerSimpleBlockItem(CONDUIT);
        for (DyeColor color : DyeColor.values()) {
            COLORED_SKINS.put(color, registerSkin(color));
        }
    }

    private PufferfishBlocks() {}

    /// Returns the registry path for the given skin color, or the undyed skin if null.
    public static String skinName(@Nullable DyeColor color) {
        return color == null ? "pufferfish_skin" : color.getName() + "_pufferfish_skin";
    }

    /// Returns the registered skin for the given color, or the undyed skin if null.
    public static PufferfishSkinBlock skin(@Nullable DyeColor color) {
        return (color == null ? PLAIN_SKIN : COLORED_SKINS.get(color)).get();
    }

    /// Registers a skin block and its item under the same identifier.
    private static DeferredBlock<PufferfishSkinBlock> registerSkin(@Nullable DyeColor color) {
        var block = BLOCKS.registerBlock(skinName(color), PufferfishSkinBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CARPET)
                        .mapColor(color == null ? MapColor.COLOR_YELLOW : color.getMapColor())
                        .sound(SoundType.WET_GRASS));
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
