// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.PufferfishDurability;

/// Keeps fins attached while the body flattens and the spines fold against its skin.
public final class DeflatingPufferfishModel<T extends Entity> extends PufferfishBigModel<T> {
    private final ModelPart tail;
    private int stage;

    public DeflatingPufferfishModel(ModelPart root) {
        super(root);
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-2, -0.3F, 0, 4, 0.6F, 2), PartPose.offset(0, 18, 4));
        tail = LayerDefinition.create(mesh, 32, 32).bakeRoot().getChild("tail");
    }

    public void prepare(ItemStack stack) {
        stage = PufferfishDurability.deflationStage(stack);
        root().getAllParts().forEach(ModelPart::resetPose);
        tail.resetPose();
        if (stage == 0) return;
        float width = width(stage);
        float height = height(stage);
        float depth = 1 - stage * 0.04F;
        for (var part : root().getAllParts().toList()) {
            if (part == root()) continue;
            part.x *= width;
            part.y = 18 + (part.y - 18) * height;
            part.z *= depth;
            part.xScale = width;
            part.yScale = height;
            part.zScale = depth;
        }
        float spine = stage == 1 ? 0.65F : stage == 2 ? 0.3F : 0.08F;
        for (String side : new String[] {"top", "bottom"}) {
            for (String position : new String[] {"front", "middle", "back"}) {
                var fin = root().getChild(side + "_" + position + "_fin");
                fin.yScale = spine;
                if (!position.equals("middle")) fin.xRot *= 1 + stage * 0.3F;
            }
        }
        for (String side : new String[] {"left", "right"}) {
            for (String position : new String[] {"front", "back"}) {
                var fin = root().getChild(side + "_" + position + "_fin");
                fin.xScale = spine;
                fin.yRot *= 1 + stage * 0.3F;
            }
            var fin = root().getChild(side + "_blue_fin");
            fin.xScale = 1;
            fin.yScale = 1;
            fin.zScale = 1;
        }
        tail.z = 4 * depth;
        tail.zScale = stage * 0.35F;
    }

    public static float height(int stage) { return switch (stage) { case 1 -> 0.80F; case 2 -> 0.52F; case 3 -> 0.25F; default -> 1; }; }
    public static float width(int stage) { return switch (stage) { case 1 -> 0.94F; case 2 -> 0.86F; case 3 -> 0.82F; default -> 1; }; }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        super.renderToBuffer(pose, buffer, light, overlay, color);
        if (stage > 0) tail.render(pose, buffer, light, overlay, color);
    }
}
