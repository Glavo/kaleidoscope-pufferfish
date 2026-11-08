// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/// Supplies the built-in pale fillet sprite for raw pufferfish meat.
public final class PufferfishMeatTexture {
    /// Pixel rows using transparent, outline, shadow, flesh, and highlight palette entries.
    private static final String[] PIXELS = {
            "................", ".........ooo....", ".......oossho...", "......osfhhfso..",
            ".....osfhhffso..", "....osfhhfsfso..", "...osfhhfsfhso..", "..osfhhfsfhfso..",
            "..ofhhfsfhffso..", "..ofhfsfhffsso..", "..osfsfhffsso...", "...osfhffsso....",
            "....osssso......", ".....oooo.......", "................", "................"
    };

    /// Prevents construction of the sprite factory.
    private PufferfishMeatTexture() {}

    /// Creates a 16-by-16 sprite whose image is owned by the returned sprite contents.
    public static SpriteContents create(ResourceLocation id) {
        var image = new NativeImage(16, 16, true);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < PIXELS[y].length(); x++) {
                int rgb = switch (PIXELS[y].charAt(x)) {
                    case 'o' -> 0x896D64;
                    case 's' -> 0xD4ADA1;
                    case 'f' -> 0xF0D9C9;
                    case 'h' -> 0xFFF3DF;
                    default -> -1;
                };
                if (rgb >= 0) image.setPixelRGBA(x, y, 0xFF000000 | ((rgb & 255) << 16) | (rgb & 0xFF00) | (rgb >>> 16));
            }
        }
        return new SpriteContents(id, new FrameSize(16, 16), image, ResourceMetadata.EMPTY);
    }
}
