// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.animal.IronGolem;
import org.glavo.mcmod.pufferfish.PufferfishHeadgear;
import org.glavo.mcmod.pufferfish.PufferfishGolemStack;
import org.glavo.mcmod.pufferfish.PufferfishDurability;
import org.glavo.mcmod.pufferfish.PufferfishSize;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class PufferfishGolemHeadLayer extends RenderLayer<IronGolem, IronGolemModel<IronGolem>> {
    private final ModelPart anchor = new ModelPart(List.of(), Map.of());
    private PufferfishHeadRenderer renderer;

    public PufferfishGolemHeadLayer(RenderLayerParent<IronGolem, IronGolemModel<IronGolem>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, IronGolem entity,
            float limbSwing, float limbSwingAmount, float partialTick, float age, float yaw, float pitch) {
        if (entity.isInvisible() || !PufferfishHeadgear.isWearing(entity)) return;
        if (renderer == null) renderer = new PufferfishHeadRenderer();
        pose.pushPose();
        getParentModel().root().getChild("head").translateAndRotate(pose);
        pose.translate(0, -13.0F / 16, -1.5F / 16);
        var fish = PufferfishGolemStack.decorations(entity);
        var minecraft = Minecraft.getInstance();
        var frustum = minecraft.level == entity.level() ? minecraft.levelRenderer.getFrustum() : null;
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        int effects = 0;
        for (int i = 0; i <= fish.size(); i++) {
            var stack = i == 0 ? entity.getItemBySlot(EquipmentSlot.HEAD) : fish.get(i - 1);
            if (frustum == null || visible(pose, stack, frustum, camera)) {
                renderer.render(anchor, pose, buffers, light, entity, age, stack, effects < 8);
                if (PufferfishItems.isRadiant(stack)) effects++;
            }
            pose.translate(0, -height(stack), 0);
        }
        pose.popPose();
    }

    private static float height(ItemStack stack) {
        return PufferfishSize.equipmentScale(stack)
                * (DeflatingPufferfishModel.height(PufferfishDurability.deflationStage(stack)) / 2 + 0.125F);
    }

    private static boolean visible(PoseStack pose, ItemStack stack, Frustum frustum, Vec3 camera) {
        var center = pose.last().pose().transformPosition(0, -height(stack) / 2, 0, new Vector3f());
        double radius = pose.last().pose().getScale(new Vector3f()).length() * PufferfishSize.equipmentScale(stack);
        return frustum.isVisible(new AABB(center.x + camera.x - radius, center.y + camera.y - radius, center.z + camera.z - radius,
                center.x + camera.x + radius, center.y + camera.y + radius, center.z + camera.z + radius));
    }

    /// Includes head rotation and death animation without changing collision or pathfinding bounds.
    public static AABB bounds(IronGolem golem) {
        float reach = height(golem.getItemBySlot(EquipmentSlot.HEAD));
        for (var stack : PufferfishGolemStack.decorations(golem)) reach += height(stack);
        return golem.getBoundingBoxForCulling().inflate((reach + 2) * golem.getScale());
    }
}
