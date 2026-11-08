// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.geom.ModelLayers;
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
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/// Renders a colored pufferfish head and alternating metal chain links.
public final class PufferfishFlailModel {
    /// The original fish texture used by undyed flails.
    private static final ResourceLocation FISH_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    /// The metal texture used by the chain's solid geometry.
    private static final ResourceLocation CHAIN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    /// The inflated head model, replaced when resource reload creates a new renderer.
    private final PufferfishBigModel<Entity> fish;
    /// A rectangular chain link centered on its local origin.
    private final ModelPart link;

    /// Creates model geometry using the current client model layers.
    public PufferfishFlailModel() {
        fish = new PufferfishBigModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PUFFERFISH_BIG));
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("link", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -2.5F, -0.3F, 0.6F, 5, 0.6F)
                .texOffs(0, 0).addBox(0.9F, -2.5F, -0.3F, 0.6F, 5, 0.6F)
                .texOffs(0, 0).addBox(-0.9F, -2.5F, -0.3F, 1.8F, 0.6F, 0.6F)
                .texOffs(0, 0).addBox(-0.9F, 1.9F, -0.3F, 1.8F, 0.6F, 0.6F), PartPose.ZERO);
        link = LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    /// Renders a head centered at the current origin, preserving dye and enchantment glint.
    public void renderHead(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float age) {
        var color = stack.get(KaleidoscopePufferfish.COLOR.get());
        var texture = PufferfishItems.hasNetheriteCombat(stack) ? PufferfishTextures.netheriteEntity()
                : color == null ? FISH_TEXTURE : PufferfishTextures.entity(color);
        pose.pushPose();
        pose.scale(0.8F, -0.8F, -0.8F);
        pose.translate(0, -18.0F / 16, 0);
        fish.setupAnim(null, 0, 0, age, 0, 0);
        fish.renderToBuffer(pose, ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutoutNoCull(texture),
                false, stack.hasFoil()), light, overlay);
        pose.popPose();
    }

    /// Renders a straight chain from the current origin to the given local-space offset.
    /// Zero-length and non-finite offsets produce no geometry; chains are capped at 80 links.
    public void renderChain(Vec3 offset, PoseStack pose, MultiBufferSource buffers, int light) {
        double length = offset.length();
        if (!Double.isFinite(length) || length < 0.001) return;
        int count = Math.min(80, Math.max(1, (int) Math.ceil(length / 0.22)));
        pose.pushPose();
        var direction = offset.normalize();
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0),
                new Vector3f((float) direction.x, (float) direction.y, (float) direction.z)));
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(CHAIN_TEXTURE));
        for (int i = 0; i < count; i++) {
            pose.pushPose();
            pose.translate(0, (i + 0.5) * length / count, 0);
            if ((i & 1) != 0) pose.mulPose(Axis.YP.rotationDegrees(90));
            link.render(pose, buffer, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.popPose();
    }
}
