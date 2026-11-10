// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

/// Moving, fading golden star trails independent of item geometry and body textures.
public final class RadiantPufferfishVisuals {
    private static final ResourceLocation STAR = ResourceLocation.withDefaultNamespace("textures/particle/flash.png");
    private static final int FRAMES = 48;

    private RadiantPufferfishVisuals() {}

    public static void render(ItemStack fish, PoseStack pose, MultiBufferSource buffers, int overlay) {
        render(fish, pose, buffers, overlay, Util.getMillis() / 2400.0 * Math.PI * 2);
    }

    public static void render(ItemStack fish, PoseStack pose, MultiBufferSource buffers, int overlay, double phase) {
        if (!PufferfishItems.isRadiant(fish)) return;
        pose.pushPose();
        pose.translate(0, 18.0 / 16, 0);
        renderCentered(pose, buffers, overlay, phase);
        pose.popPose();
    }

    /// Draws two perpendicular star trails around the current origin.
    public static void renderCentered(PoseStack pose, MultiBufferSource buffers, int overlay) {
        renderCentered(pose, buffers, overlay, Util.getMillis() / 2400.0 * Math.PI * 2);
    }

    private static void renderCentered(PoseStack pose, MultiBufferSource buffers, int overlay, double phase) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(45));
        var buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(STAR));
        for (int ring = 0; ring < 2; ring++) {
            for (int point = 0; point < 16; point++) {
                double angle = phase * (ring == 0 ? 1 : -1) - point * 0.14;
                float x = (float) Math.cos(angle) * 0.53F;
                float y = (float) Math.sin(angle) * 0.53F;
                float radius = 0.09F * (1 - point / 24.0F);
                int alpha = (int) (245 * (1 - point / 16.0F));
                var transform = pose.last();
                for (int corner = 0; corner < 4; corner++) {
                    boolean right = corner == 1 || corner == 2;
                    boolean bottom = corner >= 2;
                    buffer.addVertex(transform, x + (right ? radius : -radius), y + (bottom ? radius : -radius), 0)
                            .setColor(255, 190, 32, alpha).setUv(right ? 1 : 0, bottom ? 1 : 0)
                            .setUv1(overlay & 65535, overlay >>> 16).setUv2(240, 240).setNormal(transform, 0, 0, 1);
                }
            }
            pose.mulPose(Axis.YP.rotationDegrees(90));
        }
        pose.popPose();
    }

    /// Preserves the flat fish silhouette and adds an animated, translucent trail outside it.
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
                for (int ring = 0; ring < 2; ring++) for (int point = 0; point < 12; point++) {
                    double phase = frame * Math.PI * 2 / FRAMES * (ring == 0 ? 1 : -1) - point * 0.15;
                    double u = Math.cos(phase) * 7.3;
                    double v = Math.sin(phase) * 2.8;
                    double tilt = ring == 0 ? Math.PI / 4 : -Math.PI / 4;
                    int px = (int) ((8 + u * Math.cos(tilt) - v * Math.sin(tilt)) * size / 16);
                    int py = (int) ((8 + u * Math.sin(tilt) + v * Math.cos(tilt)) * size / 16);
                    for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                        int x = px + dx, y = py + dy;
                        if (x < 0 || y < 0 || x >= size || y >= size || Math.abs(dx) + Math.abs(dy) > 1) continue;
                        int original = source.getPixelRGBA(x * sourceSize.width() / size,
                                sourceFrame * sourceSize.height() + y * sourceSize.height() / size);
                        if ((original >>> 24) != 0) continue;
                        int alpha = (int) (245 * (1 - point / 12.0) * (dx == 0 && dy == 0 ? 1 : 0.4));
                        int previous = frames.getPixelRGBA(x, frame * size + y);
                        if ((previous >>> 24) >= alpha) continue;
                        frames.setPixelRGBA(x, frame * size + y, alpha << 24 | 0x20BEFF);
                    }
                }
            }
        }
        var animation = new AnimationMetadataSection(java.util.List.of(), size, size, 1, false);
        var metadata = new ResourceMetadata.Builder().put(AnimationMetadataSection.SERIALIZER, animation).build();
        return new SpriteContents(id, new FrameSize(size, size), frames, metadata);
    }
}
