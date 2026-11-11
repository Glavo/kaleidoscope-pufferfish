// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/// Requests a short main-hand cast; the server validates the held weapon and cooldown.
public record FlailAttackPayload() implements CustomPacketPayload {
    public static final FlailAttackPayload INSTANCE = new FlailAttackPayload();
    public static final Type<FlailAttackPayload> TYPE = new Type<>(PufferfishPlease.id("flail_attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlailAttackPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<FlailAttackPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (payload, context) -> PufferfishFlailItem.attack(context.player()));
    }
}
