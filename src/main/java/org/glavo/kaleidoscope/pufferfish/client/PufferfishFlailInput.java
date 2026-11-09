// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.glavo.kaleidoscope.pufferfish.FlailAttackPayload;
import org.glavo.kaleidoscope.pufferfish.PufferfishFlailItem;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

/// Replaces left-click melee and block breaking with a short tethered cast, including clicks into air.
public final class PufferfishFlailInput {
    private PufferfishFlailInput() {}

    public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (!event.isAttack() || player == null || player.isSpectator() || !PufferfishItems.isFlail(player.getMainHandItem())) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (player.isUsingItem() || player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())
                || PufferfishFlailItem.activeHead(player) != null) return;
        PacketDistributor.sendToServer(FlailAttackPayload.INSTANCE);
    }
}
