// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/// Supplies overlapping fan-shaped fish slices with a green garnish.
public final class PufferfishSashimiTexture {
    /// Pixel rows using transparency, flesh shading, curved grain, and two garnish greens.
    private static final String[] PIXELS = {
            "................", "........oooo....", "......oohhhhso..", ".....ohhfffvvso.",
            ".....ohfffvffso.", "....oooofvffso..", "...ohhhhossso...", "..ohhfffvvsoo...",
            "..ohfffvffso....", ".oooofvffso..g..", "ohhhhosso...glg.", "ohhfffvvso.gllgg",
            ".ohfffvfso..glg.", "..ossssso...gg..", "...oooo.........", "................"
    };

    /// Prevents construction of the sprite factory.
    private PufferfishSashimiTexture() {}

    /// Creates a 16-by-16 sprite with ivory or golden slices; the returned contents own the image.
    public static SpriteContents create(ResourceLocation id, boolean golden) {
        var image = new NativeImage(16, 16, true);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < PIXELS[y].length(); x++) {
                int rgb = switch (PIXELS[y].charAt(x)) {
                    case 'o' -> golden ? 0x98702E : 0x7E8A8E;
                    case 's' -> golden ? 0xD49E37 : 0xC5CDD0;
                    case 'f' -> golden ? 0xF4CF64 : 0xFFEDE3;
                    case 'h' -> golden ? 0xFFF3B0 : 0xFFFAF3;
                    case 'v' -> golden ? 0xE4AE46 : 0xE8C9C4;
                    case 'g' -> 0x4F7945;
                    case 'l' -> 0x94B960;
                    default -> -1;
                };
                if (rgb >= 0) image.setPixelRGBA(x, y, 0xFF000000 | ((rgb & 255) << 16) | (rgb & 0xFF00) | (rgb >>> 16));
            }
        }
        return new SpriteContents(id, new FrameSize(16, 16), image, ResourceMetadata.EMPTY);
    }
}
