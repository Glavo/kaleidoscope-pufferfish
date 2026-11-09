// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    public static final DeferredBlock<PlacedPufferfishBlock> PLACED_FISH = BLOCKS.registerBlock("placed_pufferfish",
            PlacedPufferfishBlock::new, BlockBehaviour.Properties.of().strength(0.2F).noOcclusion()
                    .sound(SoundType.WET_GRASS).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlacedPufferfishBlockEntity>> PLACED_FISH_ENTITY =
            BLOCK_ENTITIES.register("placed_pufferfish", () -> BlockEntityType.Builder.of(PlacedPufferfishBlockEntity::new,
                    java.util.stream.Stream.concat(java.util.stream.Stream.of(PLACED_FISH.get()),
                            PufferfishLamps.VARIANTS.values().stream().map(variant -> variant.block().get()))
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredBlock<PufferfishConduitBlock> CONDUIT = BLOCKS.registerBlock("pufferfish_conduit",
            PufferfishConduitBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CONDUIT));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PufferfishConduitBlockEntity>> CONDUIT_ENTITY =
            BLOCK_ENTITIES.register("pufferfish_conduit", () -> BlockEntityType.Builder.of(PufferfishConduitBlockEntity::new, CONDUIT.get()).build(null));
    public static final DeferredBlock<PufferfishSkinBlock> GOLDEN_SKIN = BLOCKS.registerBlock("golden_pufferfish_skin",
            PufferfishSkinBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CARPET)
                    .mapColor(MapColor.GOLD).sound(SoundType.WET_GRASS));
    public static final DeferredBlock<PufferfishSkinBlock> NETHERITE_SKIN = registerMetalSkin("netherite", MapColor.COLOR_BLACK);
    public static final DeferredBlock<PufferfishSkinBlock> HEAVY_SKIN = registerMetalSkin("heavy", MapColor.COLOR_LIGHT_BLUE);
    public static final DeferredBlock<PufferfishSkinBlock> COPPER_SKIN = registerCraftedSkin("copper", MapColor.COLOR_ORANGE);
    public static final java.util.List<DeferredBlock<PufferfishSkinBlock>> COPPER_SKINS = java.util.List.of(
            COPPER_SKIN, registerCraftedSkin("exposed_copper", MapColor.TERRACOTTA_LIGHT_GRAY),
            registerCraftedSkin("weathered_copper", MapColor.WARPED_STEM),
            registerCraftedSkin("oxidized_copper", MapColor.WARPED_NYLIUM),
            registerCraftedSkin("waxed_copper", MapColor.COLOR_ORANGE),
            registerCraftedSkin("waxed_exposed_copper", MapColor.TERRACOTTA_LIGHT_GRAY),
            registerCraftedSkin("waxed_weathered_copper", MapColor.WARPED_STEM),
            registerCraftedSkin("waxed_oxidized_copper", MapColor.WARPED_NYLIUM));
    public static final DeferredBlock<PufferfishSkinBlock> IRON_SKIN = registerCraftedSkin("iron", MapColor.METAL);
    public static final DeferredBlock<PufferfishSkinBlock> DIAMOND_SKIN = registerCraftedSkin("diamond", MapColor.DIAMOND);
    /// The placeable skin obtained from undyed pufferfish.
    private static final DeferredBlock<PufferfishSkinBlock> PLAIN_SKIN = registerSkin(null);
    /// Placeable skins indexed by the dye color of their ingredient fish.
    private static final Map<DyeColor, DeferredBlock<PufferfishSkinBlock>> COLORED_SKINS = new EnumMap<>(DyeColor.class);

    static {
        ITEMS.registerSimpleBlockItem(CONDUIT);
        ITEMS.registerSimpleBlockItem(GOLDEN_SKIN);
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

    /// Returns the skin produced by processing a whole fish, or null for unsupported items.
    @Nullable
    public static PufferfishSkinBlock skinForFish(ItemStack stack) {
        int copper = CopperPufferfish.index(stack);
        if (copper >= 0) return COPPER_SKINS.get(copper).get();
        if (stack.is(PufferfishItems.IRON.get())) return IRON_SKIN.get();
        if (stack.is(PufferfishItems.DIAMOND.get())) return DIAMOND_SKIN.get();
        if (stack.is(PufferfishItems.GOLDEN.get())) return GOLDEN_SKIN.get();
        if (stack.is(PufferfishItems.NETHERITE.get())) return NETHERITE_SKIN.get();
        if (stack.is(PufferfishItems.HEAVY.get())) return HEAVY_SKIN.get();
        return stack.is(Items.PUFFERFISH) ? skin(PufferfishColors.get(stack)) : null;
    }

    private static DeferredBlock<PufferfishSkinBlock> registerMetalSkin(String name, MapColor color) {
        var block = BLOCKS.registerBlock(name + "_pufferfish_skin", PufferfishSkinBlock::new,
                BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CARPET).mapColor(color).sound(SoundType.WET_GRASS));
        ITEMS.registerSimpleBlockItem(block, new Item.Properties().fireResistant());
        return block;
    }

    private static DeferredBlock<PufferfishSkinBlock> registerCraftedSkin(String name, MapColor color) {
        var properties = BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CARPET).mapColor(color).sound(SoundType.WET_GRASS);
        if (name.equals("copper") || name.equals("exposed_copper") || name.equals("weathered_copper")) properties.randomTicks();
        var block = BLOCKS.registerBlock(name + "_pufferfish_skin", PufferfishSkinBlock::new, properties);
        ITEMS.registerSimpleBlockItem(block);
        return block;
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
