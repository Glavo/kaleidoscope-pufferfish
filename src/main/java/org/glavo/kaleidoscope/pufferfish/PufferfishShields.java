// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/// Shields with vanilla mechanics and a permanent pufferfish skin facing.
public final class PufferfishShields {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);
    public static final Map<String, DeferredItem<ShieldItem>> VARIANTS;

    static {
        var variants = new LinkedHashMap<String, DeferredItem<ShieldItem>>();
        register(variants, "");
        for (var color : DyeColor.values()) register(variants, color.getName() + "_");
        register(variants, "golden_");
        VARIANTS = java.util.Collections.unmodifiableMap(variants);
    }

    private PufferfishShields() {}

    private static void register(Map<String, DeferredItem<ShieldItem>> variants, String prefix) {
        variants.put(prefix + "pufferfish_skin_shield", ITEMS.register(prefix + "pufferfish_skin_shield",
                () -> new ShieldItem(new Item.Properties().durability(336)) {
                    @Override
                    public String getDescriptionId(ItemStack stack) { return getDescriptionId(); }
                }));
    }

    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
