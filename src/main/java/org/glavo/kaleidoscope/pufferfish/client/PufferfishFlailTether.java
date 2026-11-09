// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/// Maps the world-rendered head into either hand pass using its actual camera and item transforms.
public final class PufferfishFlailTether {
    private static Matrix4f worldToClip;
    private static Vec3 cameraPosition;
    private static float partialTick;

    private PufferfishFlailTether() {}

    public static void beginFrame(RenderFrameEvent.Pre event) {
        worldToClip = null;
    }

    public static void captureWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY && event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        worldToClip = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        cameraPosition = event.getCamera().getPosition();
        partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
    }

    public static float partialTick(float fallback) { return worldToClip == null ? fallback : partialTick; }

    /// Places the first-person shoulder behind the camera independently of the player's view pitch.
    public static boolean transformToView(PoseStack pose) {
        if (worldToClip == null) return false;
        pose.mulPose(new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose()).invert());
        return true;
    }

    /// Maps a world position into the fully transformed item pass.
    public static Vec3 localPosition(Vec3 position, PoseStack pose) {
        if (worldToClip == null) return null;
        var relativeHead = position.subtract(cameraPosition);
        var clip = worldToClip.transform(new Vector4f((float) relativeHead.x, (float) relativeHead.y, (float) relativeHead.z, 1));
        var local = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix())
                .mul(pose.last().pose()).invert().transform(clip);
        if (!Float.isFinite(local.w) || Math.abs(local.w) < 1.0e-6F) return null;
        return new Vec3(local.x / local.w, local.y / local.w, local.z / local.w);
    }
}
