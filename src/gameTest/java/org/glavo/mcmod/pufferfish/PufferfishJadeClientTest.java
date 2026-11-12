// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.EntityType;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
import snownee.jade.api.Accessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.impl.BlockAccessorImpl;
import snownee.jade.impl.EntityAccessorImpl;
import snownee.jade.impl.Tooltip;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.config.PluginConfig;

public final class PufferfishJadeClientTest {
    public static void verify(Minecraft minecraft) {
        var level = ClientTestLevel.level(minecraft);
        var id = PufferfishPlease.id("level");
        for (var type : List.of(EntityType.PUFFERFISH, PufferfishMobs.VARIANTS.get("radiant_pufferfish").entity().get())) {
            var fish = type.create(level);
            var provider = WailaClientRegistration.instance().getEntityProviders(fish, value -> true).stream()
                    .filter(value -> value.getUid().equals(id)).findFirst().orElseThrow();
            var accessor = new EntityAccessorImpl.Builder().level(level).entity(() -> fish).serverConnected(true).build();
            verifyTooltip(provider, accessor);
        }
        var blocks = new java.util.ArrayList<PlacedPufferfishBlock>();
        blocks.add(PufferfishBlocks.PLACED_FISH.get());
        blocks.add(PufferfishBlocks.PLACED_HEAVY_FISH.get());
        PufferfishLamps.VARIANTS.values().forEach(variant -> blocks.add(variant.block().get()));
        for (var block : blocks) {
            var state = block.defaultBlockState();
            var entity = block.newBlockEntity(BlockPos.ZERO, state);
            var provider = WailaClientRegistration.instance().getBlockProviders(block, value -> true).stream()
                    .filter(value -> value.getUid().equals(id)).findFirst().orElseThrow();
            var accessor = new BlockAccessorImpl.Builder().level(level).blockState(state).blockEntity(() -> entity).serverConnected(true).build();
            verifyTooltip(provider, accessor);
        }
    }

    private static <T extends Accessor<?>> void verifyTooltip(IComponentProvider<T> provider, T accessor) {
        var empty = new Tooltip();
        provider.appendTooltip(empty, accessor, PluginConfig.INSTANCE);
        if (!empty.isEmpty()) throw new IllegalStateException("Jade invented a rank without server data");
        for (var name : List.of(PufferfishLevel.LEVEL_0.displayName(), PufferfishLevel.LEVEL_1.displayName(),
                new PufferfishCultivation(PufferfishLevel.LEVEL_4, 0.5, 4000, 8000).displayName(),
                new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.25, 20_000_000, 40_000_000).displayName())) {
            accessor.getServerData().put(PufferfishPlease.id("level").toString(), ComponentSerialization.CODEC.encodeStart(accessor.nbtOps(), name).getOrThrow());
            var tooltip = new Tooltip();
            provider.appendTooltip(tooltip, accessor, PluginConfig.INSTANCE);
            if (tooltip.size() != 1 || !tooltip.getMessage().trim().equals(Component.translatable("tooltip.pufferfish_please.level", name).getString())) {
                throw new IllegalStateException("Jade rendered an incorrect rank tooltip");
            }
        }
    }
}
