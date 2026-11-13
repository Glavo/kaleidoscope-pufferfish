// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.animal.IronGolem;
import org.glavo.mcmod.pufferfish.PufferfishHeadgear;

public final class PufferfishGolemHeadLayer extends RenderLayer<IronGolem, IronGolemModel<IronGolem>> {
    private final ModelPart anchor = new ModelPart(List.of(), Map.of());
    private PufferfishHeadRenderer renderer;

    public PufferfishGolemHeadLayer(RenderLayerParent<IronGolem, IronGolemModel<IronGolem>> parent) {
        super(parent);
        anchor.setPos(0, -13, -1.5F);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, IronGolem entity,
            float limbSwing, float limbSwingAmount, float partialTick, float age, float yaw, float pitch) {
        if (entity.isInvisible() || !PufferfishHeadgear.isWearing(entity)) return;
        if (renderer == null) renderer = new PufferfishHeadRenderer();
        pose.pushPose();
        getParentModel().root().getChild("head").translateAndRotate(pose);
        renderer.render(anchor, pose, buffers, light, entity, age);
        pose.popPose();
    }
}
