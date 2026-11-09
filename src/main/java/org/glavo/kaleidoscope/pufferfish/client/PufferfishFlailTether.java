// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishFlailHead;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/// Connects the first-person grip to the world-rendered head using both passes' actual transforms.
public final class PufferfishFlailTether {
    private static Matrix4f worldToClip;
    private static Vec3 cameraPosition;
    private static float partialTick;

    private PufferfishFlailTether() {}

    public static void beginFrame(RenderFrameEvent.Pre event) {
        worldToClip = null;
    }

    public static void captureWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        worldToClip = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        cameraPosition = event.getCamera().getPosition();
        partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
    }

    public static boolean usesHandPass(PufferfishFlailHead head) {
        var minecraft = Minecraft.getInstance();
        return head.getOwner() == minecraft.player && minecraft.player != null
                && minecraft.getCameraEntity() == minecraft.player && minecraft.options.getCameraType().isFirstPerson()
                && !minecraft.options.hideGui && !minecraft.player.isSpectator();
    }

    /// Called at the grip origin after all item, hand, equip, swing, and camera transforms.
    public static void render(PufferfishFlailModel model, PufferfishFlailHead head, PoseStack pose,
            MultiBufferSource buffers, int light) {
        if (worldToClip == null || !usesHandPass(head)) return;
        var relativeHead = head.getPosition(partialTick).subtract(cameraPosition);
        var clip = worldToClip.transform(new Vector4f((float) relativeHead.x, (float) relativeHead.y, (float) relativeHead.z, 1));
        var local = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix())
                .mul(pose.last().pose()).invert().transform(clip);
        if (!Float.isFinite(local.w) || Math.abs(local.w) < 1.0e-6F) return;
        var end = new Vec3(local.x / local.w, local.y / local.w, local.z / local.w);
        var offset = end.subtract(0, 0.20, 0);
        double length = offset.length();
        if (!Double.isFinite(length) || length < 0.001) return;
        pose.pushPose();
        pose.translate(0, 0.20, 0);
        model.renderChain(head.getItem(), offset.scale(Math.max(0, length - 0.20) / length), 0, pose, buffers, light);
        pose.popPose();
    }
}
