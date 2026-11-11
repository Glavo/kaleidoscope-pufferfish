// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.phys.AABB;

/// A camera-locked apparition, shown only to the cursed player.
public final class PufferfishCurseParticle extends Particle {
    private final PufferfishBigModel<Pufferfish> model;
    private final RenderType renderType = RenderType.entityTranslucent(
            ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png"));

    public PufferfishCurseParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        model = new PufferfishBigModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PUFFERFISH_BIG));
        gravity = 0;
        lifetime = 30;
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) { return AABB.INFINITE; }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float progress = Mth.clamp((age + partialTicks) / lifetime, 0, 1);
        float pulse = Mth.sin(progress * Mth.PI);
        var pose = new PoseStack();
        pose.mulPose(camera.rotation());
        pose.translate(0, 0, -1.5);
        pose.mulPose(Axis.XP.rotationDegrees(-20 + 40 * progress));
        float scale = 1.4F + 1.2F * pulse;
        pose.scale(scale, -scale, -scale);
        pose.translate(0, -18.0 / 16, 0);
        model.setupAnim(null, 0, 0, age + partialTicks, 0, 0);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        model.renderToBuffer(pose, buffers.getBuffer(renderType), 15728880, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.colorFromFloat(0.65F * pulse, 1, 1, 1));
        buffers.endBatch(renderType);
    }
}
