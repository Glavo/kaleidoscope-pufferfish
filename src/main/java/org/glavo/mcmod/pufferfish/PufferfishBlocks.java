// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PufferfishPlease.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PufferfishPlease.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PufferfishPlease.MODID);
    public static final DeferredBlock<FishProcessingTableBlock> PROCESSING_TABLE = BLOCKS.registerBlock("fish_processing_table",
            FishProcessingTableBlock::new, BlockBehaviour.Properties.of().strength(2.5F).noOcclusion().sound(SoundType.WOOD)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FishProcessingTableBlockEntity>> PROCESSING_TABLE_ENTITY =
            BLOCK_ENTITIES.register("fish_processing_table", () -> BlockEntityType.Builder.of(FishProcessingTableBlockEntity::new, PROCESSING_TABLE.get()).build(null));
    public static final DeferredBlock<PufferfishPumpBlock> PUMP = BLOCKS.registerBlock("pufferfish_pump",
            PufferfishPumpBlock::new, BlockBehaviour.Properties.of().strength(1.5F).noOcclusion().sound(SoundType.COPPER)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PufferfishPumpBlockEntity>> PUMP_ENTITY =
            BLOCK_ENTITIES.register("pufferfish_pump", () -> BlockEntityType.Builder.of(PufferfishPumpBlockEntity::new, PUMP.get()).build(null));
    public static final DeferredBlock<PlacedPufferfishBlock> PLACED_FISH = BLOCKS.registerBlock("placed_pufferfish",
            PlacedPufferfishBlock::new, BlockBehaviour.Properties.of().strength(0.2F).noOcclusion().lightLevel(state -> state.getValue(PlacedPufferfishBlock.LIT) ? 15 : 0)
                    .sound(SoundType.WET_GRASS).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    public static final DeferredBlock<HeavyPufferfishBlock> PLACED_HEAVY_FISH = BLOCKS.registerBlock("placed_heavy_pufferfish",
            HeavyPufferfishBlock::new, BlockBehaviour.Properties.of().strength(0.2F).noOcclusion()
                    .sound(SoundType.WET_GRASS).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlacedPufferfishBlockEntity>> PLACED_FISH_ENTITY =
            BLOCK_ENTITIES.register("placed_pufferfish", () -> BlockEntityType.Builder.of(PlacedPufferfishBlockEntity::new,
                    java.util.stream.Stream.concat(java.util.stream.Stream.of(PLACED_FISH.get(), PLACED_HEAVY_FISH.get()),
                            PufferfishLamps.VARIANTS.values().stream().map(variant -> variant.block().get()))
                            .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredBlock<PufferfishConduitBlock> CONDUIT = BLOCKS.registerBlock("pufferfish_conduit",
            PufferfishConduitBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CONDUIT));
    public static final DeferredBlock<PufferfishConduitBlock> ANCIENT_CONDUIT = BLOCKS.registerBlock("ancient_pufferfish_conduit",
            PufferfishConduitBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CONDUIT).noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PufferfishConduitBlockEntity>> CONDUIT_ENTITY =
            BLOCK_ENTITIES.register("pufferfish_conduit", () -> BlockEntityType.Builder.of(PufferfishConduitBlockEntity::new,
                    CONDUIT.get(), ANCIENT_CONDUIT.get()).build(null));
    public static final DeferredBlock<Block> ANCIENT_PRISMARINE = BLOCKS.registerBlock("ancient_prismarine",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).mapColor(MapColor.COLOR_CYAN));
    public static final DeferredBlock<SuspiciousAncientPrismarineBlock> SUSPICIOUS_ANCIENT_PRISMARINE =
            BLOCKS.registerBlock("suspicious_ancient_prismarine", SuspiciousAncientPrismarineBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).mapColor(MapColor.COLOR_CYAN)
                            .noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AncientPrismarineBlockEntity>> ANCIENT_PRISMARINE_ENTITY =
            BLOCK_ENTITIES.register("suspicious_ancient_prismarine", () -> BlockEntityType.Builder.of(
                    AncientPrismarineBlockEntity::new, SUSPICIOUS_ANCIENT_PRISMARINE.get()).build(null));
    private static final DeferredBlock<PufferfishSkinBlock> PLAIN_SKIN = registerSkin(PufferfishVariant.NORMAL, MapColor.COLOR_YELLOW);
    private static final Map<DyeColor, DeferredBlock<PufferfishSkinBlock>> COLORED_SKINS = registerColoredSkins();
    public static final DeferredBlock<PufferfishSkinBlock> COPPER_SKIN = registerSkin(PufferfishVariant.COPPER.get(0), MapColor.COLOR_ORANGE);
    public static final java.util.List<DeferredBlock<PufferfishSkinBlock>> COPPER_SKINS = java.util.List.of(
            COPPER_SKIN, registerSkin(PufferfishVariant.COPPER.get(1), MapColor.TERRACOTTA_LIGHT_GRAY),
            registerSkin(PufferfishVariant.COPPER.get(2), MapColor.WARPED_STEM),
            registerSkin(PufferfishVariant.COPPER.get(3), MapColor.WARPED_NYLIUM),
            registerSkin(PufferfishVariant.COPPER.get(4), MapColor.COLOR_ORANGE),
            registerSkin(PufferfishVariant.COPPER.get(5), MapColor.TERRACOTTA_LIGHT_GRAY),
            registerSkin(PufferfishVariant.COPPER.get(6), MapColor.WARPED_STEM),
            registerSkin(PufferfishVariant.COPPER.get(7), MapColor.WARPED_NYLIUM));
    public static final DeferredBlock<PufferfishSkinBlock> IRON_SKIN = registerSkin(PufferfishVariant.IRON, MapColor.METAL);
    public static final DeferredBlock<PufferfishSkinBlock> GOLDEN_SKIN = registerSkin(PufferfishVariant.GOLDEN, MapColor.GOLD);
    public static final DeferredBlock<PufferfishSkinBlock> DIAMOND_SKIN = registerSkin(PufferfishVariant.DIAMOND, MapColor.DIAMOND);
    public static final DeferredBlock<PufferfishSkinBlock> NETHERITE_SKIN = registerSkin(PufferfishVariant.NETHERITE, MapColor.COLOR_BLACK);
    public static final DeferredBlock<PufferfishSkinBlock> HEAVY_SKIN = registerSkin(PufferfishVariant.HEAVY, MapColor.COLOR_LIGHT_BLUE);
    public static final DeferredBlock<PufferfishSkinBlock> RADIANT_SKIN = registerSkin(PufferfishVariant.RADIANT, MapColor.QUARTZ);

    static {
        ITEMS.register("fish_processing_table", () -> new net.minecraft.world.item.BlockItem(PROCESSING_TABLE.get(), new Item.Properties()) {
            @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                    java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                for (var action : java.util.List.of("cut", "brush", "retrieve")) {
                    tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.pufferfish_please.processing_table." + action)
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
        });
        ITEMS.registerSimpleBlockItem(CONDUIT);
        ITEMS.registerSimpleBlockItem(ANCIENT_CONDUIT);
        ITEMS.registerSimpleBlockItem(ANCIENT_PRISMARINE);
        ITEMS.registerSimpleBlockItem(SUSPICIOUS_ANCIENT_PRISMARINE);
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
        var variant = PufferfishVariant.of(stack);
        return PufferfishItems.isPufferfish(stack) && variant != null ? variant.skin() : null;
    }

    private static Map<DyeColor, DeferredBlock<PufferfishSkinBlock>> registerColoredSkins() {
        var skins = new EnumMap<DyeColor, DeferredBlock<PufferfishSkinBlock>>(DyeColor.class);
        PufferfishVariant.DYED.forEach((color, variant) -> skins.put(color, registerSkin(variant, color.getMapColor())));
        return java.util.Collections.unmodifiableMap(skins);
    }

    private static DeferredBlock<PufferfishSkinBlock> registerSkin(PufferfishVariant variant, MapColor color) {
        var properties = BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CARPET).mapColor(color).sound(SoundType.WET_GRASS);
        if (variant.material() == PufferfishMaterial.COPPER && !variant.waxed() && variant.oxidation() < 3) properties.randomTicks();
        var block = BLOCKS.registerBlock(variant.skinName(), PufferfishSkinBlock::new, properties);
        ITEMS.register(variant.skinName(), () -> {
            var itemProperties = new Item.Properties();
            if (variant.material().fireResistant()) itemProperties.fireResistant();
            return variant.bind(new net.minecraft.world.item.BlockItem(block.get(), itemProperties));
        });
        return block;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
