// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import org.glavo.mcmod.pufferfish.entity.PufferfishBoat;

public final class PufferfishBoatRenderer extends BoatRenderer {
    private final PufferfishBoatModel skinModel = new PufferfishBoatModel();
    public PufferfishBoatRenderer(EntityRendererProvider.Context context) { super(context, false); }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        return Pair.of(PufferfishPlease.id("textures/entity/boat/" + ((PufferfishBoat) boat).skinBoatName() + ".png"),
                skinModel);
    }
}
