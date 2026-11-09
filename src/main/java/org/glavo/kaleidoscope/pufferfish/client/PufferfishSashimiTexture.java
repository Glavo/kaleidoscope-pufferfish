// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/// Supplies overlapping fan-shaped fish slices.
public final class PufferfishSashimiTexture {
    /// Pixel rows using transparency, flesh shading, and curved grain.
    private static final String[] PIXELS = {
            "................", "........oooo....", "......oohhhhso..", ".....ohhfffvvso.",
            ".....ohfffvffso.", "....oooofvffso..", "...ohhhhossso...", "..ohhfffvvsoo...",
            "..ohfffvffso....", ".oooofvffso.....", "ohhhhosso......", "ohhfffvvso......",
            ".ohfffvfso......", "..ossssso.......", "...oooo.........", "................"
    };

    /// Prevents construction of the sprite factory.
    private PufferfishSashimiTexture() {}

    /// Creates a 16-by-16 sprite with ivory slices; the returned contents own the image.
    public static SpriteContents create(ResourceLocation id) {
        var image = new NativeImage(16, 16, true);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < PIXELS[y].length(); x++) {
                int rgb = switch (PIXELS[y].charAt(x)) {
                    case 'o' -> 0x7E8A8E;
                    case 's' -> 0xC5CDD0;
                    case 'f' -> 0xFFEDE3;
                    case 'h' -> 0xFFFAF3;
                    case 'v' -> 0xE8C9C4;
                    default -> -1;
                };
                if (rgb >= 0) image.setPixelRGBA(x, y, 0xFF000000 | ((rgb & 255) << 16) | (rgb & 0xFF00) | (rgb >>> 16));
            }
        }
        return new SpriteContents(id, new FrameSize(16, 16), image, ResourceMetadata.EMPTY);
    }
}
