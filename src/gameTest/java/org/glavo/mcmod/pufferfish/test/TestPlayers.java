// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.test;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import org.jetbrains.annotations.Nullable;

public final class TestPlayers {
    private TestPlayers() {}

    /// Looks up a mod adventure advancement, failing explicitly if its resource is missing.
    public static AdvancementHolder advancement(ServerPlayer player, String id) {
        var location = PufferfishPlease.id("adventure/" + id);
        @Nullable var advancement = player.server.getAdvancements().get(location);
        if (advancement == null) throw new AssertionError("Missing advancement: " + location);
        return advancement;
    }

    /// Checks completion without treating a missing advancement as an unmet condition.
    public static boolean hasAdvancement(ServerPlayer player, String id) {
        return player.getAdvancements().getOrStartProgress(advancement(player, id)).isDone();
    }

    /// Creates an untracked survival player whose outgoing packets are discarded.
    public static ServerPlayer serverPlayer(GameTestHelper helper) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-player"), ClientInformation.createDefault());
        connect(player, packet -> {});
        return player;
    }

    public static void connect(ServerPlayer player, Consumer<Packet<?>> packets) {
        player.connection = new ServerGamePacketListenerImpl(player.server, new Connection(PacketFlow.SERVERBOUND),
                player, CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(Packet<?> packet) { packets.accept(packet); }
        };
        player.setGameMode(GameType.SURVIVAL);
    }

    /// Removes login protection without ticking unrelated player behavior.
    public static void allowDamage(ServerPlayer player) {
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not remove spawn invulnerability", exception);
        }
    }
}
