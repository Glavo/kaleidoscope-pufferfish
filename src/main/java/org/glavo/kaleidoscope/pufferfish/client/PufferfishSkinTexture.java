// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/// Unfolds the inflated pufferfish model's back and lower flank into a tileable skin.
public final class PufferfishSkinTexture {
    /// Prevents construction of this texture utility.
    private PufferfishSkinTexture() {}

    /// Applies the matching metal fish pattern before sampling a skin or equipment texture.
    public static void recolorSource(NativeImage image, String name) {
        if (name.startsWith("netherite_")) PufferfishTextures.recolor(image, PufferfishTextures.NETHERITE_COLOR);
        else if (name.startsWith("heavy_")) HeavyPufferfishVisuals.recolor(image, true);
        else if (name.startsWith("copper_") || name.startsWith("iron_") || name.startsWith("diamond_")) {
            MaterialPufferfishTexture.recolor(image, name.substring(0, name.indexOf('_')), true);
        }
    }

    /// Creates a skin sprite with a dorsal center and pale belly edges, excluding eyes and fins.
    /// Each frame has identical opposing edge pixels: the lengthwise pattern is mirrored,
    /// and both belly edges sample the same flank in opposite directions.
    /// Source frames use the vanilla entity texture's 32-by-32 UV layout; larger frames retain
    /// their resolution. Animation frame layout and metadata are retained. The source image
    /// is closed before this method returns or throws; the returned sprite owns the output image.
    ///
    /// @param id the output sprite identifier
    /// @param size the dimensions of one source animation frame
    /// @param source the entity texture, whose ownership is transferred to this method
    /// @param metadata the source animation metadata
    /// @param color the RGB body tint, or null to retain the original skin color
    /// @return the generated skin sprite
    public static SpriteContents create(ResourceLocation id, FrameSize size, NativeImage source,
            ResourceMetadata metadata, @Nullable Integer color) {
        try (source) {
            recolorSource(source, id.getPath().substring(id.getPath().lastIndexOf('/') + 1));
            int width = Math.max(1, size.width() / 2);
            int height = Math.max(1, size.height() / 2);
            var image = new NativeImage(source.getWidth() / size.width() * width,
                    source.getHeight() / size.height() * height, false);
            try {
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int localX = x % width;
                        int localY = y % height;
                        double across = localX * 16.0 / width;
                        double edge = Math.min(localX, width - 1 - localX) * 16.0 / width;
                        double along = Math.min(localY, height - 1 - localY) * 16.0 / height;
                        double u;
                        double v;
                        if (edge < 4) {
                            u = along;
                            v = 16 - edge - 16.0 / width;
                        } else {
                            u = across + 4;
                            v = along;
                        }
                        int sourceX = x / width * size.width() + (int) (u * size.width() / 32);
                        int sourceY = y / height * size.height() + (int) (v * size.height() / 32);
                        int pixel = source.getPixelRGBA(sourceX, sourceY);
                        if (id.getPath().equals("block/golden_pufferfish_skin")) {
                            pixel = tint(pixel, edge < 4 ? 0xFFF2A6 : 0xFFC52B);
                        } else if (color != null && edge >= 4) pixel = tint(pixel, color);
                        image.setPixelRGBA(x, y, pixel);
                    }
                }
                return new SpriteContents(id, new FrameSize(width, height), image, metadata);
            } catch (RuntimeException | Error exception) {
                image.close();
                throw exception;
            }
        }
    }

    /// Tints a dorsal pixel using its brightness while retaining its alpha channel.
    private static int tint(int pixel, int color) {
        int brightness = Math.max(pixel & 255, Math.max(pixel >>> 8 & 255, pixel >>> 16 & 255));
        int red = (color >>> 16 & 255) * brightness / 255;
        int green = (color >>> 8 & 255) * brightness / 255;
        int blue = (color & 255) * brightness / 255;
        return pixel & 0xFF000000 | blue << 16 | green << 8 | red;
    }
}
