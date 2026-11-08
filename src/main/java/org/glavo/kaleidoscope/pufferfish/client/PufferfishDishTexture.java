// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/// Supplies glazed fish and rice bowl sprites with a shared porcelain bowl and green garnish.
public final class PufferfishDishTexture {
    /// Pixel rows for sauced fish pieces above a shaded porcelain bowl.
    private static final String[] DISH = {
            "................", "................", ".....oooooo.....", "...ooaaaggaoo...",
            "..oahhaahhaaso..", ".oahfaahfaahaso.", ".oaafggaafggaao.", ".osaahhaahhaaso.",
            "..osaaaaaaaso...", "..odwwwwwwwdo...", "...odwwwwwdo....", "....odwwwdo.....",
            ".....odddo......", "......ooo.......", "................", "................"
    };
    /// Pixel rows exposing ivory rice beneath the same glazed fish pieces.
    private static final String[] RICE = {
            "................", "................", ".....oooooo.....", "...oorrrggroo...",
            "..orhhaahhrrso..", ".orhfaahfarrhso.", ".orrfggrrfggrao.", ".osrrhhaahrrrso.",
            "..osrrrrrrrso...", "..odwwwwwwwdo...", "...odwwwwwdo....", "....odwwwdo.....",
            ".....odddo......", "......ooo.......", "................", "................"
    };

    /// Prevents construction of this sprite factory.
    private PufferfishDishTexture() {}

    /// Creates a 16-by-16 dish sprite, optionally with rice; the returned contents own the image.
    public static SpriteContents create(ResourceLocation id, boolean rice) {
        var rows = rice ? RICE : DISH;
        var image = new NativeImage(16, 16, true);
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                int rgb = switch (rows[y].charAt(x)) {
                    case 'o' -> 0x493C3B;
                    case 'a' -> 0x8E3E27;
                    case 'h' -> 0xE9A34E;
                    case 'f' -> 0xC96B30;
                    case 's' -> 0x784C35;
                    case 'g' -> 0x70984A;
                    case 'r' -> 0xF8E7BB;
                    case 'w' -> 0xE1ECEA;
                    case 'd' -> 0x8DAAAF;
                    default -> -1;
                };
                if (rgb >= 0) image.setPixelRGBA(x, y, 0xFF000000 | (rgb & 255) << 16 | rgb & 0xFF00 | rgb >>> 16);
            }
        }
        return new SpriteContents(id, new FrameSize(16, 16), image, ResourceMetadata.EMPTY);
    }
}
