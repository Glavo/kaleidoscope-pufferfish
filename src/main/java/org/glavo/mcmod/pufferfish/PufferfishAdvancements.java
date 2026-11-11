// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishAdvancements {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, PufferfishPlease.MODID);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> WAX_PUFFERFISH = TRIGGERS.register("wax_pufferfish", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> RESTORE_ANCIENT_CONDUIT = TRIGGERS.register("restore_ancient_conduit", PlayerTrigger::new);

    private PufferfishAdvancements() {}

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PufferfishAdvancements::placedFrame);
    }

    private static void placedFrame(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) return;
        var level = player.serverLevel();
        var placedPos = event.getPos().immutable();
        var placed = event.getPlacedBlock();
        if (!placed.is(PufferfishConduitBlockEntity.ANCIENT_FRAME) && !placed.isConduitFrame(level, placedPos, placedPos)) return;
        var previous = event.getBlockSnapshot().getState();
        // Wait until placement listeners have finished so canceled placements cannot grant progress.
        level.getServer().tell(new TickTask(level.getServer().getTickCount(), () -> {
            if (event.isCanceled() || player.isRemoved() || player.level() != level) return;
            for (var pos : BlockPos.betweenClosed(placedPos.offset(-2, -2, -2), placedPos.offset(2, 2, 2))) {
                if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof PufferfishConduitBlockEntity core) || !core.isAncient()) continue;
                if (previous.is(PufferfishConduitBlockEntity.ANCIENT_FRAME) || previous.isConduitFrame(level, placedPos, pos)) continue;
                var frame = new ArrayList<BlockPos>();
                if (PufferfishConduitBlockEntity.updateFrame(level, pos, frame) && frame.size() == 42 && frame.contains(placedPos)) {
                    RESTORE_ANCIENT_CONDUIT.get().trigger(player);
                    return;
                }
            }
        }));
    }
}
