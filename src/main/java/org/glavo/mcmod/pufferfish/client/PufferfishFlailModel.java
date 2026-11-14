// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.joml.Quaternionf;

/// Renders a colored pufferfish head and alternating metal chain links.
public final class PufferfishFlailModel {
    /// The metal texture used by the chain's solid geometry.
    private static final ResourceLocation CHAIN_TEXTURE = PufferfishPlease.id("textures/entity/flail/iron.png");
    /// Dark alloy fittings with worn edges for reinforced flails.
    private static final ResourceLocation NETHERITE_TEXTURE = PufferfishPlease.id("textures/entity/flail/netherite.png");
    /// Leather wraps shared by the held model and inventory icon.
    private static final ResourceLocation GRIP_TEXTURE = PufferfishPlease.id("textures/entity/flail/grip.png");
    /// The inflated head model, replaced when resource reload creates a new renderer.
    private final DeflatingPufferfishModel<Entity> fish;
    /// A rectangular chain link centered on its local origin.
    private final ModelPart link;
    private final ModelPart grip;
    private final ModelPart bands;

    /// Creates model geometry using the current client model layers.
    public PufferfishFlailModel() {
        fish = new DeflatingPufferfishModel<>(Minecraft.getInstance().getEntityModels());
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("link", CubeListBuilder.create()
                .texOffs(0, 2).addBox(-0.5F, -1, -0.1F, 0.2F, 2, 0.2F)
                .texOffs(6, 7).addBox(0.3F, -1, -0.1F, 0.2F, 2, 0.2F)
                .texOffs(2, 2).addBox(-0.3F, -1, -0.1F, 0.6F, 0.2F, 0.2F)
                .texOffs(2, 7).addBox(-0.3F, 0.8F, -0.1F, 0.6F, 0.2F, 0.2F), PartPose.ZERO);
        link = LayerDefinition.create(mesh, 16, 16).bakeRoot();
        var handle = new MeshDefinition();
        handle.getRoot().addOrReplaceChild("grip", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.9F, -4, -0.9F, 1.8F, 7, 1.8F), PartPose.ZERO);
        grip = LayerDefinition.create(handle, 16, 16).bakeRoot();
        var metal = new MeshDefinition();
        metal.getRoot().addOrReplaceChild("bands", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.1F, -4.2F, -1.1F, 2.2F, 0.8F, 2.2F)
                .texOffs(0, 0).addBox(-1.1F, 2.4F, -1.1F, 2.2F, 0.8F, 2.2F), PartPose.ZERO);
        bands = LayerDefinition.create(metal, 16, 16).bakeRoot();
    }

    private static ResourceLocation metalTexture(ItemStack stack) {
        return PufferfishItems.hasNetheriteCombat(stack) ? NETHERITE_TEXTURE : CHAIN_TEXTURE;
    }

    private static int metalColor(ItemStack stack) {
        return PufferfishItems.hasNetheriteCombat(stack) ? -1 : 0xFFABB6C4;
    }

    public void renderGrip(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        grip.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(GRIP_TEXTURE)), light, overlay);
        bands.render(pose, ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutoutNoCull(metalTexture(stack)),
                false, stack.hasFoil()), light, overlay, metalColor(stack));
    }

    /// Renders a head centered at the current origin, preserving dye and enchantment glint.
    public void renderHead(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float age) {
        renderHead(stack, pose, buffers, light, overlay, age, 0,
                stack.getItem() instanceof org.glavo.mcmod.pufferfish.PufferfishFlailItem
                        ? org.glavo.mcmod.pufferfish.PufferfishSize.equipmentScale(stack) : 1);
    }

    public void renderHead(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float age, float pressure) {
        renderHead(stack, pose, buffers, light, overlay, age, pressure, 1);
    }

    /// Renders the fish body and its matching aura at the same scale.
    public void renderHead(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float age, float pressure, float scale) {
        renderBody(stack, pose, buffers, light, overlay, age, pressure, scale);
        if (PufferfishItems.isRadiant(stack)) {
            pose.pushPose();
            pose.scale(0.8F * scale, -0.8F * scale, -0.8F * scale);
            RadiantPufferfishVisuals.renderCentered(pose, buffers, overlay);
            pose.popPose();
        }
    }

    /// Renders the centered fish body without an aura, allowing inventory effects to use their own scale.
    public void renderBody(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float age, float pressure, float scale) {
        fish.prepare(stack, pressure);
        stack = PufferfishItems.flailFish(stack);
        var texture = PufferfishTextures.entity(stack);
        pose.pushPose();
        pose.scale(0.8F * scale, -0.8F * scale, -0.8F * scale);
        pose.translate(0, -18.0F / 16, 0);
        fish.setupAnim(null, 0, 0, age, 0, 0);
        fish.renderToBuffer(pose, ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutoutNoCull(texture),
                false, stack.hasFoil()), light, overlay);
        pose.popPose();
    }

    /// Renders interlocking links along a hanging curve with fixed endpoints.
    /// Zero-length and non-finite offsets produce no geometry; chains are capped at 96 links.
    public void renderChain(ItemStack stack, Vec3 offset, double sag, PoseStack pose, MultiBufferSource buffers, int light) {
        double length = offset.length();
        if (!Double.isFinite(length) || !Double.isFinite(sag) || length < 0.001) return;
        sag = Math.max(0, Math.min(sag, length * 0.35));
        int count = Math.min(96, Math.max(1, (int) Math.ceil((length + sag) / 0.10)));
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(metalTexture(stack)));
        int color = metalColor(stack);
        var rotation = new Quaternionf();
        var start = chainPoint(offset, sag, 0);
        for (int i = 0; i < count; i++) {
            var end = chainPoint(offset, sag, (double) (i + 1) / count);
            var direction = end.subtract(start).normalize();
            pose.pushPose();
            pose.translate((start.x + end.x) * 0.5, (start.y + end.y) * 0.5, (start.z + end.z) * 0.5);
            pose.mulPose(rotation.rotationTo(0, 1, 0, (float) direction.x, (float) direction.y, (float) direction.z));
            if ((i & 1) != 0) pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale(1, (float) (start.distanceTo(end) / 0.10), 1);
            link.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
            pose.popPose();
            start = end;
        }
    }

    private static Vec3 chainPoint(Vec3 offset, double sag, double t) {
        return offset.scale(t).add(0, -4 * sag * t * (1 - t), 0);
    }
}
