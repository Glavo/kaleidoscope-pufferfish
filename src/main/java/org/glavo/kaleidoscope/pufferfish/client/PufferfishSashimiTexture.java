// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/// Supplies a built-in sprite of three thin ivory fish slices with pink grain and a green garnish.
public final class PufferfishSashimiTexture {
    /// Pixel rows using transparency, cool outlines, flesh shading, and two garnish greens.
    private static final String[] PIXELS = {
            "................", "...........oooo.", "..........ohhhho", ".........ohhffso",
            "........ohhffso.", ".......oooosso..", "......ohhhhooo..", ".....ohhffso....",
            "....ohhffso.....", "...oooosso......", "..ohhhhooo..gg..", ".ohhffso...glgg.",
            "ohhffso...gllgg.", ".osssso....ggg..", "..oooo..........", "................"
    };

    /// Prevents construction of the sprite factory.
    private PufferfishSashimiTexture() {}

    /// Creates a 16-by-16 sprite whose image is owned by the returned sprite contents.
    public static SpriteContents create(ResourceLocation id) {
        var image = new NativeImage(16, 16, true);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < PIXELS[y].length(); x++) {
                int rgb = switch (PIXELS[y].charAt(x)) {
                    case 'o' -> 0x72858C;
                    case 's' -> 0xBAC9CC;
                    case 'f' -> 0xEBD2CD;
                    case 'h' -> 0xFFF8EC;
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
