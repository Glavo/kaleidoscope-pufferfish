// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/// A narrow skin hull with tapered ends and separate wooden paddles.
public final class PufferfishBoatModel extends BoatModel {
    public PufferfishBoatModel() { super(layer().bakeRoot()); }

    private static LayerDefinition layer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-12, 3, -6, 24, 2, 12)
                .addBox(-16, 3, -4, 4, 2, 8).addBox(12, 3, -4, 4, 2, 8)
                .addBox(-19, 2, -2, 3, 2, 4).addBox(16, 2, -2, 3, 2, 4), PartPose.ZERO);
        for (int side : new int[] {-1, 1}) {
            root.addOrReplaceChild(side < 0 ? "right" : "left", CubeListBuilder.create().texOffs(0, 18)
                    .addBox(-12, -3, side * 6 - 0.75F, 24, 7, 1.5F)
                    .texOffs(0, 40).addBox(-12, -3.5F, side * 6 - 1, 24, 1, 2), PartPose.ZERO);
            var end = root.addOrReplaceChild(side < 0 ? "back" : "front", CubeListBuilder.create(), PartPose.ZERO);
            for (int edge : new int[] {-1, 1}) {
                float angle = side * edge * (float) Math.atan2(6, 8);
                end.addOrReplaceChild("panel_" + edge, CubeListBuilder.create().texOffs(0, 18)
                        .addBox(side < 0 ? -10 : 0, -3, -0.75F, 10, 6, 1.5F)
                        .texOffs(0, 40).addBox(side < 0 ? -10 : 0, -3.5F, -1, 10, 1, 2),
                        PartPose.offsetAndRotation(side * 12, 0, edge * 6, 0, angle, 0));
            }
        }
        paddle(root, "left_paddle", 7, false);
        paddle(root, "right_paddle", -7, true);
        root.addOrReplaceChild("water_patch", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-12, -3, -6, 24, 1, 12).addBox(-16, -3, -4, 4, 1, 8).addBox(12, -3, -4, 4, 1, 8), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void paddle(PartDefinition root, String name, float z, boolean mirror) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(80, 0)
                .addBox(-0.5F, -0.5F, -5, 1, 1, 17)
                .texOffs(80, 24).addBox(-0.5F, -2, 10, 0.75F, 4, 6),
                PartPose.offsetAndRotation(3, -4, z, 0, mirror ? (float) Math.PI : 0, (float) Math.PI / 16));
    }
}
