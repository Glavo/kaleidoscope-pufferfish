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
import net.minecraft.world.phys.Vec3;
import org.glavo.kaleidoscope.pufferfish.PufferfishDurability;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
    private final ModelPart hose = part(CubeListBuilder.create().texOffs(0, 0).addBox(-0.3F, -0.3F, 0, 0.6F, 0.6F, 16));
    private final ModelPart nozzle = part(CubeListBuilder.create().texOffs(0, 0).addBox(-0.24F, -0.24F, 0, 0.48F, 0.48F, 16));
    private final ModelPart needle = part(CubeListBuilder.create().texOffs(0, 0).addBox(-0.06F, -0.06F, 0, 0.12F, 0.12F, 16));

    private static ModelPart part(CubeListBuilder cubes) {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("part", cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    public void render(ItemStack stack, float compression, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        render(stack, ItemStack.EMPTY, compression, pose, buffers, light, overlay);
    }

    public void render(ItemStack stack, ItemStack fish, float compression, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        render(base, "copper_block", stack, pose, buffers, light, overlay);
        var mouth = mouth(fish);
        var outlet = new Vec3(0, 1.35 / 16, 3.35 / 16);
        var bend = new Vec3(5.0 / 16, 1.35 / 16, 4.5 / 16);
        var coupling = mouth.add(0, 0, 0.08);
        var tipBase = mouth.add(0, 0, 0.055);
        segment(hose, "black_wool", stack, outlet, bend, pose, buffers, light, overlay);
        segment(hose, "black_wool", stack, bend, coupling, pose, buffers, light, overlay);
        segment(nozzle, "copper_block", stack, coupling, tipBase, pose, buffers, light, overlay);
        segment(needle, "iron_block", stack, tipBase, mouth.add(0, 0, -0.01), pose, buffers, light, overlay);
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

    public static Vec3 fishCenter(ItemStack fish) {
        float height = DeflatingPufferfishModel.height(PufferfishDurability.deflationStage(fish));
        return new Vec3(5.0 / 16, 1.0 / 16 + height * 0.10, 0);
    }

    /// Mouth centers from the vanilla front-face UVs after the cradle's 0.4 model scale.
    public static Vec3 mouth(ItemStack fish) {
        int stage = PufferfishDurability.deflationStage(fish);
        return fishCenter(fish).add(0, stage == 0 ? 0.0125 : 0, DeflatingPufferfishModel.height(stage) * 0.10);
    }

    private static void segment(ModelPart part, String texture, ItemStack stack, Vec3 start, Vec3 end,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var direction = end.subtract(start);
        pose.pushPose();
        pose.translate(start.x, start.y, start.z);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), direction.normalize().toVector3f()));
        pose.scale(1, 1, (float) direction.length());
        render(part, texture, stack, pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void render(ModelPart part, String texture, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        part.render(pose, ItemRenderer.getFoilBufferDirect(buffers,
                RenderType.entityCutoutNoCull(ResourceLocation.withDefaultNamespace("textures/block/" + texture + ".png")),
                false, stack.hasFoil()), light, overlay);
    }
}
