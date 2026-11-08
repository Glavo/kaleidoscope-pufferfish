// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
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
    public static final DeferredBlock<Block> NETHERITE = BLOCKS.registerSimpleBlock("netherite_pufferfish_block",
            BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK));
    private static final DeferredBlock<Block> PLAIN = registerBlock(null);
    private static final Map<DyeColor, DeferredBlock<Block>> COLORED = new EnumMap<>(DyeColor.class);
    /// The carpet made from undyed pufferfish.
    private static final DeferredBlock<CarpetBlock> PLAIN_CARPET = registerCarpet(null);
    /// Carpets indexed by the dye color of their ingredient fish.
    private static final Map<DyeColor, DeferredBlock<CarpetBlock>> COLORED_CARPETS = new EnumMap<>(DyeColor.class);

    static {
        ITEMS.registerSimpleBlockItem(CONDUIT);
        ITEMS.registerSimpleBlockItem(NETHERITE, new Item.Properties().fireResistant());
        for (DyeColor color : DyeColor.values()) {
            COLORED.put(color, registerBlock(color));
            COLORED_CARPETS.put(color, registerCarpet(color));
        }
    }

    private PufferfishBlocks() {}

    public static String name(@Nullable DyeColor color) {
        return color == null ? "pufferfish_block" : color.getName() + "_pufferfish_block";
    }

    public static Block get(@Nullable DyeColor color) {
        return (color == null ? PLAIN : COLORED.get(color)).get();
    }

    private static DeferredBlock<Block> registerBlock(@Nullable DyeColor color) {
        var block = BLOCKS.registerSimpleBlock(name(color), BlockBehaviour.Properties.of()
                .mapColor(color == null ? MapColor.COLOR_YELLOW : color.getMapColor())
                .strength(0.5F).sound(SoundType.WET_GRASS));
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    /// Returns the registry path for the given carpet color, or the undyed carpet if null.
    public static String carpetName(@Nullable DyeColor color) {
        return color == null ? "pufferfish_carpet" : color.getName() + "_pufferfish_carpet";
    }

    /// Returns the registered carpet for the given color, or the undyed carpet if null.
    public static CarpetBlock carpet(@Nullable DyeColor color) {
        return (color == null ? PLAIN_CARPET : COLORED_CARPETS.get(color)).get();
    }

    /// Registers a carpet and its item with vanilla carpet shape and support requirements.
    private static DeferredBlock<CarpetBlock> registerCarpet(@Nullable DyeColor color) {
        var block = BLOCKS.registerBlock(carpetName(color), CarpetBlock::new,
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
