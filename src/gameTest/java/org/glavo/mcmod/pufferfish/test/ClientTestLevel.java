// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.test;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Lifecycle;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerLinks;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.neoforged.neoforge.network.connection.ConnectionType;

public final class ClientTestLevel {
    private ClientTestLevel() {}

    private static ClientLevel level;
    private static ClientPacketListener connection;

    public static ClientPacketListener connection(Minecraft minecraft) {
        level(minecraft);
        return connection;
    }

    private static <T> Registry<T> copyRegistry(HolderLookup.Provider lookup, ResourceKey<? extends Registry<T>> key) {
        var registry = new MappedRegistry<T>(key, Lifecycle.stable());
        lookup.lookupOrThrow(key).listElements().forEach(holder -> registry.register(holder.key(), holder.value(), RegistrationInfo.BUILT_IN));
        return registry.freeze();
    }

    public static ClientLevel level(Minecraft minecraft) {
        if (level == null) {
            var lookup = VanillaRegistries.createLookup();
            var dynamic = Stream.of(
                    new RegistryAccess.RegistryEntry<>(Registries.DAMAGE_TYPE, copyRegistry(lookup, Registries.DAMAGE_TYPE)),
                    new RegistryAccess.RegistryEntry<>(Registries.DIMENSION_TYPE, copyRegistry(lookup, Registries.DIMENSION_TYPE)),
                    new RegistryAccess.RegistryEntry<>(Registries.BIOME, copyRegistry(lookup, Registries.BIOME)));
            var access = new RegistryAccess.ImmutableRegistryAccess(Stream.concat(
                    RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registries(), dynamic)).freeze();
            var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), "client-test"), null,
                    access, FeatureFlags.DEFAULT_FLAGS, null, null, null, Map.of(), null, false, Map.of(),
                    ServerLinks.EMPTY, ConnectionType.NEOFORGE);
            connection = new ClientPacketListener(minecraft, new Connection(PacketFlow.CLIENTBOUND), cookie);
            level = new ClientLevel(connection, new ClientLevel.ClientLevelData(Difficulty.NORMAL, false, false),
                    Level.OVERWORLD, access.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
                    2, 2, minecraft::getProfiler, minecraft.levelRenderer, false, 0);
        }
        return level;
    }
}
