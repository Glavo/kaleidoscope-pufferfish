// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.glavo.mcmod.pufferfish.PufferfishHeadgear;
import org.glavo.mcmod.pufferfish.PufferfishItems;

public final class PufferfishHeadRenderer {
    private final DeflatingPufferfishModel<LivingEntity> model = new DeflatingPufferfishModel<>(
            Minecraft.getInstance().getEntityModels());

    public static boolean shouldRender(LivingEntity entity) {
        return PufferfishHeadgear.isWearing(entity) || entity instanceof ArmorStand
                && PufferfishItems.isPufferfish(entity.getItemBySlot(EquipmentSlot.HEAD));
    }

    public void render(ModelPart head, PoseStack pose, MultiBufferSource buffers, int light,
            LivingEntity wearer, float ageInTicks) {
        render(head, pose, buffers, light, wearer, ageInTicks, wearer.getItemBySlot(EquipmentSlot.HEAD), true);
    }

    public void render(ModelPart head, PoseStack pose, MultiBufferSource buffers, int light,
            LivingEntity wearer, float ageInTicks, net.minecraft.world.item.ItemStack stack, boolean radiantEffects) {
        var texture = PufferfishTextures.entity(stack);
        pose.pushPose();
        if (wearer.isBaby()) {
            pose.scale(0.75F, 0.75F, 0.75F);
            pose.translate(0, 1, 0);
        }
        head.translateAndRotate(pose);
        float scale = org.glavo.mcmod.pufferfish.PufferfishSize.equipmentScale(stack);
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -22.0F / 16.0F, 0.0F);
        pose.translate(0, (1 - DeflatingPufferfishModel.height(org.glavo.mcmod.pufferfish.PufferfishDurability.deflationStage(stack))) / 4, 0);
        model.prepare(stack);
        model.setupAnim(wearer, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light,
                LivingEntityRenderer.getOverlayCoords(wearer, 0.0F));
        if (radiantEffects) RadiantPufferfishVisuals.render(stack, pose, buffers, LivingEntityRenderer.getOverlayCoords(wearer, 0.0F));
        pose.popPose();
    }

}
