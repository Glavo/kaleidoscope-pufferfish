// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.Collections;
import java.util.IdentityHashMap;
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
import org.jetbrains.annotations.Nullable;

public final class PufferfishLamps {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PufferfishPlease.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PufferfishPlease.MODID);
    public static final Map<String, Variant> VARIANTS;
    /// Associates registered lamp blocks with their fish without resolving deferred items.
    private static final Map<Block, PufferfishVariant> FISH_BY_BLOCK = new IdentityHashMap<>();

    static {
        var variants = new LinkedHashMap<String, Variant>();
        PufferfishVariant.ALL.stream().filter(PufferfishVariant::hasLamp).forEach(variant -> add(variants, variant));
        VARIANTS = Collections.unmodifiableMap(variants);
    }

    private PufferfishLamps() {}

    /// Registers the lamp and records its fish identity when the block is constructed.
    private static void add(Map<String, Variant> variants, PufferfishVariant variant) {
        String name = variant.fishName() + "_lamp";
        int light = switch (variant.oxidation()) {
            case 1 -> 12;
            case 2 -> 8;
            case 3 -> 4;
            default -> 15;
        };
        var block = BLOCKS.registerBlock(name, settings -> {
                    var lamp = variant.material() == PufferfishMaterial.HEAVY
                            ? new HeavyPufferfishBlock(settings) : new PlacedPufferfishBlock(settings);
                    FISH_BY_BLOCK.put(lamp, variant);
                    return lamp;
                },
                BlockBehaviour.Properties.of().strength(0.3F).noOcclusion().lightLevel(state -> light).sound(SoundType.LANTERN));
        var properties = new Item.Properties();
        if (variant.material().fireResistant()) properties.fireResistant();
        ITEMS.register(name, () -> variant.bind(new net.minecraft.world.item.BlockItem(block.get(), properties)));
        variants.put(name, new Variant(block, variant::fish));
    }

    /// Recognizes registered lamps by block identity.
    public static boolean isLamp(Block block) {
        return FISH_BY_BLOCK.containsKey(block);
    }

    /// Creates a fresh fish for a lamp, or an empty stack for other blocks.
    public static ItemStack fish(Block block) {
        @Nullable var variant = FISH_BY_BLOCK.get(block);
        return variant == null ? ItemStack.EMPTY : variant.fish();
    }

    /// Creates the matching lamp for a whole fish; derivatives and unsupported variants have no result.
    public static ItemStack lamp(ItemStack fish) {
        @Nullable var variant = PufferfishVariant.of(fish);
        if (variant == null || fish.isEmpty() || fish.getItem() != variant.fishItem() || !variant.hasLamp()) return ItemStack.EMPTY;
        return new ItemStack(VARIANTS.get(variant.fishName() + "_lamp").block().get());
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public record Variant(DeferredBlock<PlacedPufferfishBlock> block, Supplier<ItemStack> fish) {}
}
