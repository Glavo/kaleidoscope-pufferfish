// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishBoat;

public final class PufferfishBoats {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, KaleidoscopePufferfish.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);
    public record Variant(DeferredHolder<EntityType<?>, EntityType<PufferfishBoat>> entity, DeferredItem<PufferfishBoatItem> item) {}
    public static final Map<String, Variant> VARIANTS;

    static {
        var variants = new LinkedHashMap<String, Variant>();
        add(variants, "");
        for (var color : DyeColor.values()) add(variants, color.getName() + "_");
        add(variants, "golden_");
        add(variants, "netherite_");
        add(variants, "heavy_");
        VARIANTS = Collections.unmodifiableMap(variants);
    }

    private PufferfishBoats() {}

    private static void add(Map<String, Variant> variants, String prefix) {
        String name = prefix + "pufferfish_skin_boat";
        var entity = ENTITIES.register(name, () -> EntityType.Builder.<PufferfishBoat>of(
                (type, level) -> new PufferfishBoat(type, level, name), MobCategory.MISC)
                .sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10)
                .build(KaleidoscopePufferfish.id(name).toString()));
        var item = ITEMS.register(name, () -> {
            var properties = new Item.Properties().stacksTo(1);
            if (prefix.equals("netherite_") || prefix.equals("heavy_")) properties.fireResistant();
            return new PufferfishBoatItem(entity, properties);
        });
        variants.put(name, new Variant(entity, item));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(PufferfishBoats::setup);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> VARIANTS.values().forEach(variant ->
                DispenserBlock.registerBehavior(variant.item().get(), new PufferfishBoatItem.Dispenser(variant.entity()))));
    }
}
