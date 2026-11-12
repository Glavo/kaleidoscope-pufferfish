// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.EntityType;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
import snownee.jade.impl.EntityAccessorImpl;
import snownee.jade.impl.Tooltip;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.config.PluginConfig;

public final class PufferfishJadeClientTest {
    public static void verify(Minecraft minecraft) {
        var level = ClientTestLevel.level(minecraft);
        for (var type : List.of(EntityType.PUFFERFISH, PufferfishMobs.VARIANTS.get("radiant_pufferfish").entity().get())) {
            var fish = type.create(level);
            var id = PufferfishPlease.id("level");
            var provider = WailaClientRegistration.instance().getEntityProviders(fish, value -> true).stream()
                    .filter(value -> value.getUid().equals(id)).findFirst().orElseThrow();
            var accessor = new EntityAccessorImpl.Builder().level(level).entity(() -> fish).serverConnected(true).build();
            var empty = new Tooltip();
            provider.appendTooltip(empty, accessor, PluginConfig.INSTANCE);
            if (!empty.isEmpty()) throw new IllegalStateException("Jade invented a rank without server data");
            for (var name : List.of(PufferfishLevel.LEVEL_0.displayName(), PufferfishLevel.LEVEL_1.displayName(),
                    new PufferfishCultivation(PufferfishLevel.LEVEL_4, 0.5, 4000, 8000).displayName(),
                    new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.25, 20_000_000, 40_000_000).displayName())) {
                var data = new CompoundTag();
                data.put(id.toString(), ComponentSerialization.CODEC.encodeStart(accessor.nbtOps(), name).getOrThrow());
                var client = new EntityAccessorImpl.Builder().from(accessor).serverData(data).build();
                var tooltip = new Tooltip();
                provider.appendTooltip(tooltip, client, PluginConfig.INSTANCE);
                if (tooltip.size() != 1 || !tooltip.getMessage().trim().equals(Component.translatable("tooltip.pufferfish_please.level", name).getString())) {
                    throw new IllegalStateException("Jade rendered an incorrect rank tooltip");
                }
            }
        }
    }
}
