// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.util.FastColor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.PufferfishItems;

/// Drifting golden wisps, independent of body textures and collision geometry.
public final class RadiantPufferfishVisuals {
    private static final ResourceLocation STAR = ResourceLocation.withDefaultNamespace("textures/particle/flash.png");
    private static final int FRAMES = 80;
    private static final int SEGMENTS = 20;
    private static final double ARC_SPAN = 1.05;
    private static final int GOLD = 0xFFCA38;
    private static final int CORE = 0xFFF0AC;

    private RadiantPufferfishVisuals() {}

    public static void render(ItemStack fish, PoseStack pose, MultiBufferSource buffers, int overlay) {
        render(fish, pose, buffers, overlay, Util.getMillis() / 8000.0 * Math.PI * 2);
    }

    public static void render(ItemStack fish, PoseStack pose, MultiBufferSource buffers, int overlay, double phase) {
        if (!PufferfishItems.isRadiant(fish)) return;
        pose.pushPose();
        pose.translate(0, 18.0 / 16, 0);
        renderCentered(pose, buffers, overlay, phase);
        pose.popPose();
    }

    /// Short light arcs drift, breathe and fade without outlining a rigid ring.
    public static void renderCentered(PoseStack pose, MultiBufferSource buffers, int overlay) {
        renderCentered(pose, buffers, overlay, Util.getMillis() / 8000.0 * Math.PI * 2);
    }

    private static void renderCentered(PoseStack pose, MultiBufferSource buffers, int overlay, double phase) {
        for (int arc = 0; arc < 3; arc++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees((float) (arc * 55 + 25 * Math.sin(phase + arc * 2))));
            pose.mulPose(Axis.XP.rotationDegrees((float) (20 + arc * 25 + 22 * Math.sin(phase + arc))));
            pose.translate(0, 0.035 * Math.sin(phase * 2 + arc * 2), 0);
            double center = arcPhase(phase, arc);
            float radius = (float) (0.58 + arc * 0.02 + 0.045 * Math.sin(phase + arc * 2));
            float width = (float) (0.075 + 0.015 * Math.sin(phase * 2 + arc));
            double strength = arcStrength(phase, arc);
            var glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(PufferfishTextures.radiantGlow()));
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double start = -ARC_SPAN + segment * ARC_SPAN * 2 / SEGMENTS;
                double end = -ARC_SPAN + (segment + 1) * ARC_SPAN * 2 / SEGMENTS;
                float startWidth = width * (float) Math.sqrt(arcLight(start));
                float endWidth = width * (float) Math.sqrt(arcLight(end));
                int startAlpha = (int) (arcLight(start) * strength * 235);
                int endAlpha = (int) (arcLight(end) * strength * 235);
                orbitVertex(glow, pose.last(), start + center, radius - startWidth, 0, startAlpha, overlay);
                orbitVertex(glow, pose.last(), end + center, radius - endWidth, 0, endAlpha, overlay);
                orbitVertex(glow, pose.last(), end + center, radius + endWidth, 1, endAlpha, overlay);
                orbitVertex(glow, pose.last(), start + center, radius + startWidth, 1, startAlpha, overlay);
            }
            double leading = center + 0.35;
            float x = (float) Math.cos(leading) * radius;
            float y = (float) Math.sin(leading) * radius;
            float sparkle = (float) (0.025 + 0.012 * strength);
            var stars = buffers.getBuffer(RenderType.entityTranslucentEmissive(STAR));
            for (int corner = 0; corner < 4; corner++) {
                boolean right = corner == 1 || corner == 2;
                boolean bottom = corner >= 2;
                stars.addVertex(pose.last(), x + (right ? sparkle : -sparkle), y + (bottom ? sparkle : -sparkle), 0)
                        .setColor(FastColor.ARGB32.color((int) (195 * strength), CORE)).setUv(right ? 1 : 0, bottom ? 1 : 0)
                        .setUv1(overlay & 65535, overlay >>> 16).setUv2(240, 240).setNormal(pose.last(), 0, 0, 1);
            }
            pose.popPose();
        }
    }

    private static double arcPhase(double phase, int arc) {
        return phase * (arc == 1 ? -1 : 1) + arc * Math.PI * 2 / 3 + 0.25 * Math.sin(phase * 2 + arc);
    }

    private static double arcStrength(double phase, int arc) {
        return Math.pow((1 + Math.sin(phase + arc * Math.PI * 2 / 3)) * 0.5, 0.7) * (1 - arc * 0.12);
    }

    private static double arcLight(double angle) {
        return Math.max(0, (Math.cos(angle) - Math.cos(ARC_SPAN)) / (1 - Math.cos(ARC_SPAN)));
    }

    private static void orbitVertex(VertexConsumer buffer, PoseStack.Pose pose, double angle, float radius, float v, int alpha, int overlay) {
        buffer.addVertex(pose, (float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, 0)
                .setColor(255, 255, 255, alpha).setUv(0.5F, v)
                .setUv1(overlay & 65535, overlay >>> 16).setUv2(240, 240).setNormal(pose, 0, 0, 1);
    }

    private static int glowColor(double distance) {
        double core = Math.exp(-distance * distance * 35);
        double edge = Math.max(0, 1 - distance * distance);
        int alpha = (int) (255 * (0.45 * core + 0.55 * edge * edge));
        int color = FastColor.ARGB32.lerp((float) core, GOLD | 0xFF000000, CORE | 0xFF000000);
        return FastColor.ARGB32.color(alpha, color);
    }

    public static NativeImage createGlowTexture() {
        var image = new NativeImage(2, 32, true);
        for (int y = 0; y < 32; y++) for (int x = 0; x < 2; x++) {
            image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(glowColor((y - 15.5) / 15.5)));
        }
        return image;
    }

    /// Preserves the flat fish silhouette and adds the same breathing, drifting arcs outside it.
    public static SpriteContents createItemSprite(ResourceLocation id, FrameSize sourceSize, NativeImage source) {
        int size = Math.max(32, sourceSize.width());
        var frames = new NativeImage(size, size * FRAMES, true);
        try (source) {
            for (int frame = 0; frame < FRAMES; frame++) {
                int sourceFrame = frame % Math.max(1, source.getHeight() / sourceSize.height());
                for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                    frames.setPixelRGBA(x, frame * size + y, source.getPixelRGBA(x * sourceSize.width() / size,
                            sourceFrame * sourceSize.height() + y * sourceSize.height() / size));
                }
                for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                    if ((frames.getPixelRGBA(x, frame * size + y) >>> 24) != 0) continue;
                    double px = (x + 0.5) * 16 / size - 8, py = (y + 0.5) * 16 / size - 8;
                    int brightest = 0;
                    for (int arc = 0; arc < 3; arc++) {
                        double phase = frame * Math.PI * 2 / FRAMES;
                        double tilt = 0.3 + arc * 0.7 + 0.25 * Math.sin(phase + arc);
                        double u = px * Math.cos(tilt) + py * Math.sin(tilt);
                        double v = -px * Math.sin(tilt) + py * Math.cos(tilt);
                        double radius = 6.9 + 0.35 * Math.sin(phase + arc * 2);
                        double ellipse = Math.sqrt(u * u / (radius * radius) + v * v / (3.5 * 3.5));
                        double distance = Math.abs(ellipse - 1) * 3.5 / 0.65;
                        if (distance >= 1) continue;
                        double angle = Math.atan2(v / 3.5, u / radius);
                        int color = glowColor(distance);
                        int alpha = (int) ((color >>> 24) * arcLight(angle - arcPhase(phase, arc)) * arcStrength(phase, arc));
                        if (alpha > (brightest >>> 24)) brightest = FastColor.ARGB32.color(alpha, color);
                    }
                    frames.setPixelRGBA(x, frame * size + y, FastColor.ABGR32.fromArgb32(brightest));
                }
            }
        }
        var animation = new AnimationMetadataSection(java.util.List.of(), size, size, 2, true);
        var metadata = new ResourceMetadata.Builder().put(AnimationMetadataSection.SERIALIZER, animation).build();
        return new SpriteContents(id, new FrameSize(size, size), frames, metadata);
    }
}
