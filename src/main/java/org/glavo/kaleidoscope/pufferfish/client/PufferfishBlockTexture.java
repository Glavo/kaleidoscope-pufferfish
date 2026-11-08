// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.world.item.DyeColor;

public final class PufferfishBlockTexture {
    private PufferfishBlockTexture() {}

    public static SpriteContents create(ResourceLocation id, FrameSize size, NativeImage source,
            ResourceMetadata metadata, @Nullable DyeColor color) {
        try (source) {
            if (color != null) {
                PufferfishTextures.recolor(source, color);
            }
            var image = new NativeImage(source.getWidth() * 3, source.getHeight() * 3, false);
            int background = color == null ? 0xC6A252 : color.getTextureDiffuseColor();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int sourceX = x / (size.width() * 3) * size.width() + x % size.width();
                    int sourceY = y / (size.height() * 3) * size.height() + y % size.height();
                    int pixel = source.getPixelRGBA(sourceX, sourceY);
                    int alpha = pixel >>> 24;
                    int shade = x % size.width() == 0 || y % size.height() == 0 ? 110 : 160;
                    int red = blend(pixel & 255, (background >>> 16 & 255) * shade / 255, alpha);
                    int green = blend(pixel >>> 8 & 255, (background >>> 8 & 255) * shade / 255, alpha);
                    int blue = blend(pixel >>> 16 & 255, (background & 255) * shade / 255, alpha);
                    image.setPixelRGBA(x, y, 0xFF000000 | blue << 16 | green << 8 | red);
                }
            }
            return new SpriteContents(id, new FrameSize(size.width() * 3, size.height() * 3), image, metadata);
        }
    }

    private static int blend(int foreground, int background, int alpha) {
        return (foreground * alpha + background * (255 - alpha)) / 255;
    }
}
