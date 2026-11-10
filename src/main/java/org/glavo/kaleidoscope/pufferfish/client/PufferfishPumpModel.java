// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/// A low copper base, collapsible bellows, wooden tread, and hose to the fish cradle.
public final class PufferfishPumpModel {
    private final ModelPart base = part(CubeListBuilder.create().texOffs(0, 0).addBox(-7, 0, -6, 8, 1, 12)
            .texOffs(0, 0).addBox(3, 0, -3, 4, 1, 6));
    private final ModelPart bellows = part(CubeListBuilder.create().texOffs(0, 0).addBox(-6, 0, -5, 6, 1, 10)
            .texOffs(0, 0).addBox(-5.5F, 1, -4.5F, 5, 0.7F, 9)
            .texOffs(0, 0).addBox(-6, 1.7F, -5, 6, 1, 10)
            .texOffs(0, 0).addBox(-5.5F, 2.7F, -4.5F, 5, 0.7F, 9)
            .texOffs(0, 0).addBox(-6, 3.4F, -5, 6, 0.6F, 10));
    private final ModelPart pedal = part(CubeListBuilder.create().texOffs(0, 0).addBox(-6.5F, 0, -5.5F, 7, 1, 11));
    private final ModelPart hose = part(CubeListBuilder.create().texOffs(0, 0).addBox(0, 1, 3, 4, 0.7F, 0.7F)
            .texOffs(0, 0).addBox(3.3F, 1, 0, 0.7F, 0.7F, 3));

    private static ModelPart part(CubeListBuilder cubes) {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("part", cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    public void render(ItemStack stack, float compression, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        render(base, "copper_block", stack, pose, buffers, light, overlay);
        render(hose, "black_wool", stack, pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0, 1.0 / 16, 0);
        pose.scale(1, 1 - compression * 0.75F, 1);
        render(bellows, "brown_wool", stack, pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, (5 - compression * 3) / 16, 0);
        render(pedal, "stripped_oak_log", stack, pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void render(ModelPart part, String texture, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        part.render(pose, ItemRenderer.getFoilBufferDirect(buffers,
                RenderType.entityCutoutNoCull(ResourceLocation.withDefaultNamespace("textures/block/" + texture + ".png")),
                false, stack.hasFoil()), light, overlay);
    }
}
