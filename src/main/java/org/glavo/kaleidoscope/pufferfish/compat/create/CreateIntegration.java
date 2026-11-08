// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.create;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;

public final class CreateIntegration {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KaleidoscopePufferfish.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KaleidoscopePufferfish.MODID);

    public static final DeferredBlock<PufferfishEngineBlock> ENGINE = BLOCKS.register("pufferfish_engine",
            () -> new PufferfishEngineBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.COPPER)
                    .pushReaction(PushReaction.BLOCK).noOcclusion()));
    public static final DeferredItem<BlockItem> ENGINE_ITEM = ITEMS.register("pufferfish_engine",
            () -> new BlockItem(ENGINE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PufferfishEngineBlockEntity>> ENGINE_ENTITY =
            BLOCK_ENTITIES.register("pufferfish_engine", () -> BlockEntityType.Builder.of(PufferfishEngineBlockEntity::new, ENGINE.get()).build(null));

    public static final ModConfigSpec CONFIG;
    public static final ModConfigSpec.IntValue SPEED;
    public static final ModConfigSpec.IntValue NORMAL_CAPACITY;
    public static final ModConfigSpec.IntValue NETHERITE_CAPACITY;
    /// Total engine stress capacity in SU when powered by a heavy pufferfish.
    public static final ModConfigSpec.IntValue HEAVY_CAPACITY;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("pufferfishEngine");
        SPEED = builder.comment("Generated speed in RPM.").defineInRange("speed", 16, 1, 256);
        NORMAL_CAPACITY = builder.comment("Total stress capacity in SU with a normal or dyed pufferfish.")
                .defineInRange("normalCapacity", 256, 1, 1048576);
        NETHERITE_CAPACITY = builder.comment("Total stress capacity in SU with a netherite pufferfish.")
                .defineInRange("netheriteCapacity", 1024, 1, 1048576);
        HEAVY_CAPACITY = builder.comment("Total stress capacity in SU with a heavy pufferfish.")
                .defineInRange("heavyCapacity", 4096, 1, 1048576);
        builder.pop();
        CONFIG = builder.build();
    }

    private CreateIntegration() {}

    public static void register(IEventBus bus, ModContainer container) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, CONFIG);
    }
}
