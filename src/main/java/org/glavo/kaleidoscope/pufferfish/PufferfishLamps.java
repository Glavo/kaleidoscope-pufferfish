// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
        PufferfishVariant.ALL.stream().filter(PufferfishVariant::hasLamp).forEach(variant -> add(variants, variant));
        VARIANTS = Collections.unmodifiableMap(variants);
    }

    private PufferfishLamps() {}

    private static void add(Map<String, Variant> variants, PufferfishVariant variant) {
        String name = variant.fishName() + "_lamp";
        int light = switch (variant.oxidation()) {
            case 1 -> 12;
            case 2 -> 8;
            case 3 -> 4;
            default -> 15;
        };
        var block = BLOCKS.<PlacedPufferfishBlock>registerBlock(name,
                variant.material() == PufferfishMaterial.HEAVY ? HeavyPufferfishBlock::new : PlacedPufferfishBlock::new,
                BlockBehaviour.Properties.of().strength(0.3F).noOcclusion().lightLevel(state -> light).sound(SoundType.LANTERN));
        var properties = new Item.Properties();
        if (variant.material().fireResistant()) properties.fireResistant();
        ITEMS.register(name, () -> variant.bind(new net.minecraft.world.item.BlockItem(block.get(), properties)));
        variants.put(name, new Variant(block, variant::fish));
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
