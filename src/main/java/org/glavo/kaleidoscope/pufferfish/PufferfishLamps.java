// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishLamps {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KaleidoscopePufferfish.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);
    public static final Map<String, Variant> VARIANTS;

    static {
        var variants = new LinkedHashMap<String, Variant>();
        add(variants, "pufferfish", () -> new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) {
            add(variants, color.getName() + "_pufferfish", () -> PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        }
        add(variants, "golden_pufferfish", () -> new ItemStack(PufferfishItems.GOLDEN.get()));
        for (var fish : PufferfishItems.COPPER_VARIANTS) add(variants, fish.getId().getPath(), () -> new ItemStack(fish.get()));
        add(variants, "iron_pufferfish", () -> new ItemStack(PufferfishItems.IRON.get()));
        add(variants, "diamond_pufferfish", () -> new ItemStack(PufferfishItems.DIAMOND.get()));
        add(variants, "netherite_pufferfish", () -> new ItemStack(PufferfishItems.NETHERITE.get()));
        add(variants, "heavy_pufferfish", () -> new ItemStack(PufferfishItems.HEAVY.get()));
        VARIANTS = Collections.unmodifiableMap(variants);
    }

    private PufferfishLamps() {}

    private static void add(Map<String, Variant> variants, String fishName, Supplier<ItemStack> fish) {
        String name = fishName + "_lamp";
        int light = switch (fishName) {
            case "exposed_copper_pufferfish", "waxed_exposed_copper_pufferfish" -> 12;
            case "weathered_copper_pufferfish", "waxed_weathered_copper_pufferfish" -> 8;
            case "oxidized_copper_pufferfish", "waxed_oxidized_copper_pufferfish" -> 4;
            default -> 15;
        };
        var block = BLOCKS.<PlacedPufferfishBlock>registerBlock(name,
                fishName.equals("heavy_pufferfish") ? HeavyPufferfishBlock::new : PlacedPufferfishBlock::new,
                BlockBehaviour.Properties.of().strength(0.3F).noOcclusion().lightLevel(state -> light).sound(SoundType.LANTERN));
        var properties = new Item.Properties();
        if (fishName.equals("netherite_pufferfish") || fishName.equals("heavy_pufferfish")) properties.fireResistant();
        ITEMS.registerSimpleBlockItem(block, properties);
        variants.put(name, new Variant(block, fish));
    }

    public static boolean isLamp(Block block) {
        return VARIANTS.values().stream().anyMatch(variant -> variant.block().get() == block);
    }

    public static ItemStack fish(Block block) {
        return VARIANTS.values().stream().filter(variant -> variant.block().get() == block)
                .findFirst().map(variant -> variant.fish().get()).orElse(ItemStack.EMPTY);
    }

    public static ItemStack lamp(ItemStack fish) {
        var color = PufferfishColors.get(fish);
        String name = color != null ? color.getName() + "_pufferfish"
                : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(fish.getItem()).getPath();
        var variant = VARIANTS.get(name + "_lamp");
        return variant == null ? ItemStack.EMPTY : new ItemStack(variant.block().get());
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public record Variant(DeferredBlock<PlacedPufferfishBlock> block, Supplier<ItemStack> fish) {}
}
