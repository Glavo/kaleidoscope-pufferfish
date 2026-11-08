// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KaleidoscopePufferfish.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);
    private static final DeferredBlock<Block> PLAIN = registerBlock(null);
    private static final Map<DyeColor, DeferredBlock<Block>> COLORED = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            COLORED.put(color, registerBlock(color));
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

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
